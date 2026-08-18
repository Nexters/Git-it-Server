package com.nexters.gitit.application

import com.nexters.gitit.application.GenerateQuiz.Companion.TIMEOUT
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.QuizGenerationFinished
import com.nexters.gitit.domain.quizrepo.QuizGenerationStarter
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import com.nexters.gitit.domain.quizrepo.RepoCheckout
import com.nexters.gitit.infrastructure.github.GithubRepositoryFetcher
import com.nexters.gitit.infrastructure.quiz.AnchorLocator
import com.nexters.gitit.infrastructure.quiz.DocumentAnalyzer
import com.nexters.gitit.infrastructure.quiz.QualityInspector
import com.nexters.gitit.infrastructure.quiz.QuestionGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * 문제 저장소 하나를 채우는 유스케이스. 파이프라인 단계를 잇는 유일한 자리입니다.
 *
 * 다른 유스케이스와 달리 결과를 돌려주지 않습니다. 한 번 도는 데 몇 분(tarball 다운로드 + 해제 + 개념당 4콜)이
 * 걸리는 작업이라, 진행 상태도 산출물도 전부 `QuizRepo` 도큐먼트로 나갑니다.
 *
 * 단계 사이에 흐르는 것은 앞 단계의 산출물과 [com.nexters.gitit.domain.quizrepo.RepoCheckout.root]뿐입니다.
 * 여기서 단계 내부를 들여다보고 지름길을 내면(예: 문서 목록을 M4까지 그대로 넘기기)
 * 단계를 따로 발전시킬 수 없게 됩니다.
 */
@Service
class GenerateQuiz(
    private val quizRepoRepository: QuizRepoRepository,
    private val quizGenerationStarter: QuizGenerationStarter,
    private val githubRepositoryFetcher: GithubRepositoryFetcher,
    private val documentAnalyzer: DocumentAnalyzer,
    private val anchorLocator: AnchorLocator,
    private val questionGenerator: QuestionGenerator,
    private val qualityInspector: QualityInspector,
    private val clock: Clock,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * 대상을 실행 직전에 다시 읽습니다. 부르는 쪽이 대기 목록을 한 번 읽고 순회하는 동안 시간이 흐르므로,
     * 옛 스냅숏을 그대로 믿으면 그 사이 삭제되거나 이미 끝난 저장소를 다시 돌립니다.
     *
     * 점유하지 못했다면 남이 돌리는 중이라는 뜻이고, 그대로 돌아갑니다 — 겹쳐 들어온 쪽이 할 일은 없습니다.
     */
    operator fun invoke(command: Command) {
        val quizRepo = quizRepoRepository.findById(command.quizRepoId) ?: return

        val startedAt = Instant.now(clock)
        if (!quizRepo.start(quizGenerationStarter, startedAt, TIMEOUT)) {
            logger.info { "Quiz generation already running, skipped: quizRepoId=${quizRepo.id}" }
            return
        }

        generate(quizRepo, startedAt)
    }

    /**
     * 성공이든 거절이든 저장소 상태로 남깁니다. 결과를 반환값으로 알리지 않는 유스케이스라,
     * 도큐먼트에 적지 않으면 요청한 사용자는 영영 READY만 보게 됩니다.
     *
     * 판정이 아닌 예외는 상태만 남기고 **그대로 다시 던집니다.** 삼키면 부르는 쪽이 성공과 구분할 수
     * 없어지고, 그 예외를 어떻게 다룰지는 부르는 쪽의 정책입니다.
     *
     * 그 한 줄을 위해 catch 범위를 넓게 잡습니다. 좁히면 놓친 종류만큼 상태가 점유에 멈춥니다.
     *
     * 어느 경로로 끝나든 [QuizGenerationFinished]가 나갑니다. 사고로 끝난 것도 기다리던 사용자에게는 결과입니다.
     *
     * 시효가 지나 결과가 버려진 회차에서도 이벤트는 나갑니다. 그 경로는 좀비뿐이라, 좀비 회수를
     * 넣을 때 함께 봅니다.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun generate(
        quizRepo: QuizRepo,
        startedAt: Instant,
    ) {
        var checkout: RepoCheckout? = null

        try {
            checkout = githubRepositoryFetcher.fetch(quizRepo.githubRepoUrl)

            val anchored = anchored(quizRepo, checkout)
            val written = questionGenerator.generate(checkout.root, anchored)
            val inspected = qualityInspector.inspect(checkout.root, written)

            finish(quizRepo, startedAt) { complete(it, checkout.repo.sha, inspected) }

            logger.info { "Generated ${inspected.size} learning sets for ${quizRepo.githubRepoUrl}" }
        } catch (e: BaseException) {
            logger.warn { "Quiz generation stopped for ${quizRepo.githubRepoUrl}: ${e.errorCode.code} ${e.message}" }
            finish(quizRepo, startedAt) { reject(it, e.errorCode) }
        } catch (e: Exception) {
            finish(quizRepo, startedAt) { fail(it) }
            throw e
        } finally {
            checkout?.root?.toFile()?.deleteRecursively()
            eventPublisher.publishEvent(QuizGenerationFinished(quizRepo.id))
        }
    }

    /**
     * 시효가 지났다는 거절은 여기서 삼킵니다. 결말을 적으려다 실패한 것이라 위로 올려 봐야 할 일이 없고,
     * 사고 경로에서 다시 던지면 원래 예외를 이것이 덮어씁니다.
     *
     * 대신 걸린 시간을 남깁니다. [TIMEOUT]을 얼마나 넘겼는지가 그 값을 다시 잡을 유일한 근거입니다.
     */
    private fun finish(
        quizRepo: QuizRepo,
        startedAt: Instant,
        outcome: QuizRepo.(Instant) -> Unit,
    ) {
        val now = Instant.now(clock)

        try {
            quizRepo.outcome(now)
            quizRepoRepository.save(quizRepo)
        } catch (e: BaseException) {
            val elapsed = Duration.between(startedAt, now)
            logger.warn {
                "Quiz generation result discarded after ${elapsed.toSeconds()}s " +
                    "(timeout ${TIMEOUT.toSeconds()}s): quizRepoId=${quizRepo.id} ${e.errorCode.code}"
            }
        }
    }

    /**
     * 문서 분석·앵커를 돌리거나, 이미 돌려 둔 것을 그대로 씁니다.
     *
     * 체크포인트를 재사용하는 이유는 여기까지가 전체 콜의 절반이어서입니다. 사고로 끝난 저장소를 다시
     * 돌릴 때 그 절반이 통째로 아껴집니다. 쓸 수 있는지는 [QuizRepo.cachedAnchors]가 판정합니다.
     *
     * 이 중간 저장은 시효를 보지 않습니다. 좀비가 여기서 새 점유자의 산출물을 덮으면 한 회차를
     * 버리지만, 남는 시효가 이미 만료값이라 다음 회차가 다시 집어 갑니다.
     */
    private fun anchored(
        quizRepo: QuizRepo,
        checkout: RepoCheckout,
    ): List<AnchoredConcept> {
        val cached = quizRepo.cachedAnchors(checkout.repo.sha)
        if (cached.isNotEmpty()) return cached

        val concepts = documentAnalyzer.analyze(checkout.root)
        val anchored = anchorLocator.locate(checkout.root, concepts)
        quizRepoRepository.save(quizRepo.apply { checkpoint(checkout.repo.sha, anchored) })

        return anchored
    }

    /** 파이프라인이 알아야 할 나머지는 전부 저장소 도큐먼트 안에 있어 id 하나만 받습니다. */
    data class Command(
        val quizRepoId: String,
    )

    companion object {
        // 실측 10분(Redis·Gson 기준)의 세 배. 큰 레포에 여유를 주되, 정말 멎었을 때 영영 붙잡고 있지 않는 값이다.
        private val TIMEOUT = Duration.ofMinutes(30)
    }
}
