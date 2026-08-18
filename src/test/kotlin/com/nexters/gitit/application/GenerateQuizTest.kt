package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorKind
import com.nexters.gitit.domain.quizrepo.AnchorNote
import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.domain.quizrepo.LearningSet
import com.nexters.gitit.domain.quizrepo.QuizGenerationFinished
import com.nexters.gitit.domain.quizrepo.QuizGenerationStarter
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.domain.quizrepo.RepoCheckout
import com.nexters.gitit.domain.quizrepo.RepoCoordinates
import com.nexters.gitit.infrastructure.github.GithubRepositoryFetcher
import com.nexters.gitit.infrastructure.quiz.AnchorLocator
import com.nexters.gitit.infrastructure.quiz.DocumentAnalyzer
import com.nexters.gitit.infrastructure.quiz.QualityInspector
import com.nexters.gitit.infrastructure.quiz.QuestionGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant

class GenerateQuizTest {
    private val quizRepoRepository: QuizRepoRepository = mock()
    private val githubRepositoryFetcher: GithubRepositoryFetcher = mock()
    private val documentAnalyzer: DocumentAnalyzer = mock()
    private val anchorLocator: AnchorLocator = mock()
    private val questionGenerator: QuestionGenerator = mock()
    private val qualityInspector: QualityInspector = mock()
    private val eventPublisher: ApplicationEventPublisher = mock()

    // 점유에 성공한 경우가 기본값이다. 실패시키는 것은 겹침을 다루는 한 케이스뿐이라 거기서 다시 스텁한다.
    private val quizGenerationStarter: QuizGenerationStarter = mock { on { start(any(), any()) } doReturn true }

    private val clock: Clock = Clock.systemUTC()

    private val anchor = Anchor("src/Router.kt", 10, 20, AnchorKind.DEFINITION, "class Router")

    private val concept = Concept("라우팅", "라우팅은 Router가 전담합니다.", "README.md", listOf("src/Router.kt"))

    private val checkout = RepoCheckout(ROOT, COORDINATES)

    // 표시용 필드는 파이프라인이 읽지 않아 아무 값이나 채운다.
    private val quizRepo =
        QuizRepo(
            githubRepoId = "1310710749",
            githubRepoUrl = REPO_URL,
            name = "Git-it-Server",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
            registeredAt = Instant.EPOCH,
        )

    private val generateQuiz =
        GenerateQuiz(
            quizRepoRepository,
            quizGenerationStarter,
            githubRepositoryFetcher,
            documentAnalyzer,
            anchorLocator,
            questionGenerator,
            qualityInspector,
            clock,
            eventPublisher,
        )

    @Test
    fun `앞 단계 산출물이 그대로 다음 단계로 흐르고, 저장소에 문제가 채워진다`() {
        val concepts = listOf(concept)
        val anchored = listOf(AnchoredConcept(concept, listOf(anchor)))
        val written = listOf(learningSet("검사 전"))
        val inspected = listOf(learningSet("검사 후"))

        whenever(quizRepoRepository.findById(quizRepo.id)) doReturn quizRepo
        whenever(githubRepositoryFetcher.fetch(REPO_URL)) doReturn checkout
        // 각 단계를 "앞 단계가 내놓은 바로 그 값"으로만 스텁한다. 배선이 어긋나면 스텁이 안 걸려 여기서 죽는다.
        whenever(documentAnalyzer.analyze(ROOT)) doReturn concepts
        whenever(anchorLocator.locate(ROOT, concepts)) doReturn anchored
        whenever(questionGenerator.generate(ROOT, anchored)) doReturn written
        whenever(qualityInspector.inspect(ROOT, written)) doReturn inspected

        generateQuiz(GenerateQuiz.Command(quizRepo.id))

        quizRepo.status shouldBe QuizRepoStatus.COMPLETED
        quizRepo.sha shouldBe COORDINATES.sha
        quizRepo.learningSets shouldBe inspected
    }

    @Test
    fun `앵커까지 끝나면 문제 생성 전에 체크포인트를 남긴다`() {
        val concepts = listOf(concept)
        val anchored = listOf(AnchoredConcept(concept, listOf(anchor)))

        whenever(quizRepoRepository.findById(quizRepo.id)) doReturn quizRepo
        whenever(githubRepositoryFetcher.fetch(REPO_URL)) doReturn checkout
        whenever(documentAnalyzer.analyze(ROOT)) doReturn concepts
        whenever(anchorLocator.locate(ROOT, concepts)) doReturn anchored
        // 체크포인트가 생성 콜보다 먼저 저장되어야 의미가 있으므로, 생성이 죽는 상황으로 확인한다.
        whenever(questionGenerator.generate(ROOT, anchored)) doThrow BaseException(ErrorCode.QUESTION_GENERATION_FAILED)

        generateQuiz(GenerateQuiz.Command(quizRepo.id))

        quizRepo.anchoredConcepts shouldBe anchored
        quizRepo.sha shouldBe COORDINATES.sha
    }

    @Test
    fun `체크포인트가 있고 커밋이 같으면 문서 분석과 앵커를 다시 부르지 않는다`() {
        val anchored = listOf(AnchoredConcept(concept, listOf(anchor)))
        val written = listOf(learningSet("검사 전"))
        val inspected = listOf(learningSet("검사 후"))
        // 실제로 재개가 걸리는 경로는 사고와 재시도를 거친 도큐먼트다. 상태는 READY로 돌아가 있고, 재개 근거는 남아 있는 앵커뿐이다.
        quizRepo.apply {
            start(quizGenerationStarter, Instant.EPOCH, Duration.ofMinutes(30))
            checkpoint(COORDINATES.sha, anchored)
            fail(Instant.EPOCH)
            retry(clock)
        }

        whenever(quizRepoRepository.findById(quizRepo.id)) doReturn quizRepo
        whenever(githubRepositoryFetcher.fetch(REPO_URL)) doReturn checkout
        whenever(questionGenerator.generate(ROOT, anchored)) doReturn written
        whenever(qualityInspector.inspect(ROOT, written)) doReturn inspected

        generateQuiz(GenerateQuiz.Command(quizRepo.id))

        // 콜을 아끼는 것이 체크포인트의 전부라, 앞 단계를 부르지 않는다는 사실 자체가 검증 대상이다.
        verify(documentAnalyzer, never()).analyze(any())
        verify(anchorLocator, never()).locate(any(), any())
        quizRepo.status shouldBe QuizRepoStatus.COMPLETED
    }

    @Test
    fun `거절되면 사유를 저장소에 남기며, 예외는 유스케이스 밖으로 나가지 않는다`() {
        whenever(quizRepoRepository.findById(quizRepo.id)) doReturn quizRepo
        whenever(githubRepositoryFetcher.fetch(REPO_URL)) doReturn checkout
        whenever(documentAnalyzer.analyze(ROOT)) doThrow BaseException(ErrorCode.NO_CONCEPTS)

        // 비동기로 불려 예외를 던져도 받아줄 호출자가 없다. 삼키는 것이 설계 의도라 고정해 둔다.
        generateQuiz(GenerateQuiz.Command(quizRepo.id))

        quizRepo.status shouldBe QuizRepoStatus.REJECTED
        quizRepo.rejectedReason shouldBe ErrorCode.NO_CONCEPTS
        quizRepo.learningSets shouldBe emptyList()
    }

    @Test
    fun `판정이 아닌 예외는 실패로 남기되 그대로 다시 던진다`() {
        whenever(quizRepoRepository.findById(quizRepo.id)) doReturn quizRepo
        whenever(githubRepositoryFetcher.fetch(REPO_URL)) doThrow IllegalStateException("압축 해제 실패")

        // 삼키면 부르는 쪽이 성공과 구분할 수 없다. 상태를 남기는 것과 알리는 것은 별개다.
        shouldThrow<IllegalStateException> { generateQuiz(GenerateQuiz.Command(quizRepo.id)) }

        quizRepo.status shouldBe QuizRepoStatus.FAILED
        // 사고에는 사유 코드가 없다. reject와 섞이면 클라이언트가 설명할 수 없는 코드를 받는다.
        quizRepo.rejectedReason shouldBe null
        // 예외가 밖으로 나가는 경로에서도 알림이 나가는지가 finally로 둔 이유의 전부다.
        verify(eventPublisher).publishEvent(QuizGenerationFinished(quizRepo.id))
    }

    @Test
    fun `체크포인트를 남긴 뒤 사고가 나도 앵커는 남는다`() {
        val concepts = listOf(concept)
        val anchored = listOf(AnchoredConcept(concept, listOf(anchor)))

        whenever(quizRepoRepository.findById(quizRepo.id)) doReturn quizRepo
        whenever(githubRepositoryFetcher.fetch(REPO_URL)) doReturn checkout
        whenever(documentAnalyzer.analyze(ROOT)) doReturn concepts
        whenever(anchorLocator.locate(ROOT, concepts)) doReturn anchored
        whenever(questionGenerator.generate(ROOT, anchored)) doThrow IllegalStateException("커넥션이 끊겼습니다")

        shouldThrow<IllegalStateException> { generateQuiz(GenerateQuiz.Command(quizRepo.id)) }

        quizRepo.status shouldBe QuizRepoStatus.FAILED
        // 재시도가 앵커를 다시 만들지 않는 근거는 이 값이다. 결말을 적을 때 지워지면 절반의 콜을 다시 쓴다.
        quizRepo.anchoredConcepts shouldBe anchored
        quizRepo.sha shouldBe COORDINATES.sha
    }

    @Test
    fun `도는 동안에는 점유 상태에 머물다가 끝에서 한 번 바뀐다`() {
        val concepts = listOf(concept)
        val anchored = listOf(AnchoredConcept(concept, listOf(anchor)))
        val written = listOf(learningSet("검사 전"))

        whenever(quizRepoRepository.findById(quizRepo.id)) doReturn quizRepo
        whenever(githubRepositoryFetcher.fetch(REPO_URL)) doReturn checkout
        whenever(documentAnalyzer.analyze(ROOT)) doReturn concepts
        whenever(anchorLocator.locate(ROOT, concepts)) doReturn anchored
        // 체크포인트 저장이 상태를 건드리면 대기줄에서 빠지거나 점유가 풀린다. 끝난 뒤에 보면 마지막 값만 남아 확인할 수 없다.
        whenever(questionGenerator.generate(ROOT, anchored)).then {
            quizRepo.status shouldBe QuizRepoStatus.STARTED
            written
        }
        whenever(qualityInspector.inspect(ROOT, written)) doReturn written

        generateQuiz(GenerateQuiz.Command(quizRepo.id))

        quizRepo.status shouldBe QuizRepoStatus.COMPLETED
    }

    @Test
    fun `이미 돌고 있는 저장소는 건드리지 않는다`() {
        whenever(quizRepoRepository.findById(quizRepo.id)) doReturn quizRepo
        // 남이 이미 쥐고 있으면 조건부 갱신이 걸러 낸다 — 스케줄러와 다른 경로가 겹치는 상황이다.
        whenever(quizGenerationStarter.start(any(), any())) doReturn false

        generateQuiz(GenerateQuiz.Command(quizRepo.id))

        verify(githubRepositoryFetcher, never()).fetch(any())
        quizRepo.status shouldBe QuizRepoStatus.READY
    }

    private fun learningSet(orientation: String) =
        LearningSet(
            id = "set-1",
            concept = concept,
            title = "라우팅 흐름 따라가기",
            description = "요청이 어느 경로로 흘러가는지 확인하는 학습 세트입니다.",
            orientation = orientation,
            notes = listOf(AnchorNote(anchor, "경로를 정의하는 자리")),
            questions = emptyMap(),
        )

    companion object {
        private const val REPO_URL = "https://github.com/Nexters/Git-it-Server"
        private val COORDINATES = RepoCoordinates("Nexters", "Git-it-Server", "abc1234")
        private val ROOT: Path = Path.of("/work/Nexters-Git-it-Server-abc1234")
    }
}
