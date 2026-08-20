package com.nexters.gitit.application.quizrepo

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
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
 * 결과를 돌려주지 않습니다. 한 번 도는 데 몇 분(tarball 다운로드 + 해제 + 개념당 4콜)이 걸려,
 * 진행 상태도 산출물도 전부 `QuizRepo` 도큐먼트로 나갑니다.
 *
 * 단계 사이에 흐르는 것은 앞 단계의 산출물과 [com.nexters.gitit.domain.quizrepo.RepoCheckout.root]뿐입니다.
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
     * 성공이든 거절이든 저장소 상태로 남깁니다. 반환값이 없는 유스케이스라, 도큐먼트에 적지 않으면
     * 요청한 사용자는 영영 READY만 보게 됩니다.
     *
     * 판정이 아닌 것은 전부 FAILED입니다 — [RETRYABLE]에 걸리는 [BaseException], 그리고 예상 못 한 예외.
     * 뒤쪽은 **그대로 다시 던집니다.** 어떤 종류든 점유에 멈춘 채로 두지 않으려고 catch를 넓게 잡습니다.
     *
     * [QuizGenerationFinished]는 결말을 적은 회차만 냅니다. 시효를 잃어 버려진 결과로 알리면,
     * 회수되어 곧 다시 돌 생성을 실패로 알리게 됩니다.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun generate(
        quizRepo: QuizRepo,
        startedAt: Instant,
    ) {
        var checkout: RepoCheckout? = null
        var recorded = false

        try {
            checkout = githubRepositoryFetcher.fetch(quizRepo.githubRepoUrl)

            val anchored = anchored(quizRepo, checkout)
            val written = questionGenerator.generate(checkout.root, anchored)
            val inspected = qualityInspector.inspect(checkout.root, written)

            recorded = finish(quizRepo, startedAt) { complete(it, checkout.repo.sha, inspected) }

            logger.info { "Generated ${inspected.size} learning sets for ${quizRepo.githubRepoUrl}" }
        } catch (e: BaseException) {
            logger.warn { "Quiz generation stopped for ${quizRepo.githubRepoUrl}: ${e.errorCode.code} ${e.message}" }
            recorded = finish(quizRepo, startedAt) { if (e.errorCode in RETRYABLE) fail(it) else reject(it, e.errorCode) }
        } catch (e: Exception) {
            recorded = finish(quizRepo, startedAt) { fail(it) }
            throw e
        } finally {
            checkout?.root?.toFile()?.deleteRecursively()
            if (recorded) {
                eventPublisher.publishEvent(QuizGenerationFinished(quizRepo.id))
            }
        }
    }

    /**
     * 결말을 적었으면 true, 시효를 잃어 버렸으면 false입니다.
     *
     * 시효가 지났다는 거절은 여기서 삼키고 걸린 시간만 로그로 남깁니다. 사고 경로에서 다시 던지면
     * 원래 예외를 이것이 덮어씁니다. 로그의 경과 시간은 [TIMEOUT]을 다시 잡을 근거입니다.
     */
    private fun finish(
        quizRepo: QuizRepo,
        startedAt: Instant,
        outcome: QuizRepo.(Instant) -> Unit,
    ): Boolean {
        val now = Instant.now(clock)

        return try {
            quizRepo.outcome(now)
            quizRepoRepository.save(quizRepo)
            true
        } catch (e: BaseException) {
            val elapsed = Duration.between(startedAt, now)
            logger.warn {
                "Quiz generation result discarded after ${elapsed.toSeconds()}s " +
                    "(timeout ${TIMEOUT.toSeconds()}s): quizRepoId=${quizRepo.id} ${e.errorCode.code}"
            }
            false
        }
    }

    /**
     * 문서 분석·앵커를 돌리거나, 이미 돌려 둔 것을 그대로 씁니다. 재사용해도 되는지는
     * [QuizRepo.cachedAnchors]가 sha로 판정합니다 — 여기까지가 전체 콜의 절반이라 재시도에서 크게 아낍니다.
     *
     * 이 중간 저장은 시효를 보지 않습니다. 멎었다 깨어난 실행이 새 점유자의 산출물을 덮을 수 있지만,
     * 그 저장소는 만료된 시효를 그대로 들고 있어 회수 폴링이 곧 대기줄로 되돌립니다.
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
        // 쿼터(429)나 네트워크로 막힌 것은 판정이 아니라 사고다. REJECTED로 굳으면 retry가 거부해(FAILED만 허용) 영영 못 돌린다.
        private val RETRYABLE = setOf(ErrorCode.REPO_FETCH_FAILED)

        // 큰 레포에 넉넉히 주되, 정말 멎었을 때 영영 붙잡고 있지 않는 값. 한 회차 실측이 10분 안팎이다.
        private val TIMEOUT = Duration.ofHours(1)
    }
}
