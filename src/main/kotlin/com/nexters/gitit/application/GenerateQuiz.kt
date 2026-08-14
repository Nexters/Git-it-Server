package com.nexters.gitit.application

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
    private val eventPublisher: ApplicationEventPublisher,
) {
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
    operator fun invoke(command: Command) {
        // 등록 직후 삭제된 저장소라면 만들 대상이 없다. 되살릴 방법도 없으므로 조용히 끝낸다.
        val quizRepo = quizRepoRepository.findById(command.quizRepoId) ?: return

        try {
            val checkout = githubRepositoryFetcher.fetch(quizRepo.githubRepoUrl)

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
        if (quizRepo.status == QuizRepoStatus.ANCHORED && quizRepo.sha == checkout.repo.sha) {
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
}
