package com.nexters.gitit.application

import com.nexters.gitit.domain.common.LockManager
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.QuizGenerationFinished
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.domain.quizrepo.RepoCheckout
import com.nexters.gitit.infrastructure.github.GithubRepositoryFetcher
import com.nexters.gitit.infrastructure.quiz.AnchorLocator
import com.nexters.gitit.infrastructure.quiz.DocumentAnalyzer
import com.nexters.gitit.infrastructure.quiz.QualityInspector
import com.nexters.gitit.infrastructure.quiz.QuestionGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Duration

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
    private val githubRepositoryFetcher: GithubRepositoryFetcher,
    private val documentAnalyzer: DocumentAnalyzer,
    private val anchorLocator: AnchorLocator,
    private val questionGenerator: QuestionGenerator,
    private val qualityInspector: QualityInspector,
    private val lockManager: LockManager,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * 대상을 실행 직전에 다시 읽습니다. 부르는 쪽이 대기 목록을 한 번 읽고 순회하는 동안 시간이 흐르므로,
     * 옛 스냅숏을 그대로 믿으면 그 사이 삭제되거나 이미 끝난 저장소를 다시 돌립니다.
     */
    operator fun invoke(command: Command) {
        val quizRepo = quizRepoRepository.findById(command.quizRepoId) ?: return

        // 키가 저장소 id라 락은 저장소마다 따로 걸린다 — 다른 저장소의 생성은 서로 막지 않는다.
        lockManager.hold("$LOCK_PREFIX${quizRepo.githubRepoId}", LEASE) { generate(quizRepo) }
            ?: logger.info { "Quiz generation already running, skipped: quizRepoId=${quizRepo.id}" }
    }

    /**
     * 성공이든 거절이든 저장소 상태로 남깁니다. 결과를 반환값으로 알리지 않는 유스케이스라,
     * 도큐먼트에 적지 않으면 요청한 사용자는 영영 READY만 보게 됩니다.
     *
     * 판정이 아닌 예외는 상태만 남기고 **그대로 다시 던집니다.** 삼키면 부르는 쪽이 성공과 구분할 수
     * 없어지고, 그 예외를 어떻게 다룰지는 부르는 쪽의 정책입니다.
     *
     * 그 한 줄을 위해 catch 범위를 넓게 잡습니다. 좁히면 놓친 종류만큼 상태가 READY에 멈춥니다.
     *
     * 어느 경로로 끝나든 [QuizGenerationFinished]가 나갑니다. 사고로 끝난 것도 기다리던 사용자에게는 결과입니다.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun generate(quizRepo: QuizRepo) {
        var checkout: RepoCheckout? = null

        try {
            checkout = githubRepositoryFetcher.fetch(quizRepo.githubRepoUrl)

            val anchored = anchored(quizRepo, checkout)
            val written = questionGenerator.generate(checkout.root, anchored)
            val inspected = qualityInspector.inspect(checkout.root, written)

            quizRepo.complete(checkout.repo.sha, inspected)
            quizRepoRepository.save(quizRepo)

            logger.info { "Generated ${inspected.size} learning sets for ${quizRepo.githubRepoUrl}" }
        } catch (e: BaseException) {
            logger.warn { "Quiz generation stopped for ${quizRepo.githubRepoUrl}: ${e.errorCode.code} ${e.message}" }
            quizRepo.reject(e.errorCode)
            quizRepoRepository.save(quizRepo)
        } catch (e: Exception) {
            quizRepo.fail()
            quizRepoRepository.save(quizRepo)
            throw e
        } finally {
            checkout?.root?.toFile()?.deleteRecursively()
            eventPublisher.publishEvent(QuizGenerationFinished(quizRepo.id))
        }
    }

    /**
     * 문서 분석·앵커를 돌리거나, 이미 돌려 둔 것을 그대로 씁니다.
     *
     * 체크포인트를 재사용하는 이유는 여기까지가 전체 콜의 절반이어서입니다. 대신 커밋이 같을 때로 한정합니다 —
     * 앵커는 라인 번호라, 레포가 갱신된 뒤 옛 앵커를 쓰면 이미 게이트를 통과한 값이라서 뒷단계 검증에도
     * 안 걸린 채 엉뚱한 코드를 인용하게 됩니다.
     */
    private fun anchored(
        quizRepo: QuizRepo,
        checkout: RepoCheckout,
    ): List<AnchoredConcept> {
        // 재시도가 상태를 READY로 되돌리므로 status로는 앵커 보유를 알 수 없다. failedFrom이 그 표식이다.
        if (quizRepo.failedFrom == QuizRepoStatus.ANCHORED && quizRepo.sha == checkout.repo.sha) {
            return quizRepo.anchoredConcepts
        }

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
        private const val LOCK_PREFIX = "quiz-generation:"

        // 실측 10분(Redis·Gson 기준)의 세 배. 큰 레포에 여유를 주되, 정말 멎었을 때 영영 붙잡고 있지 않는 값이다.
        private val LEASE = Duration.ofMinutes(30)
    }
}
