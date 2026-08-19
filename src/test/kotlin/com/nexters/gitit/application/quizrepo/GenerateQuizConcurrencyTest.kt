package com.nexters.gitit.application.quizrepo

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorKind
import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.domain.quizrepo.QuizGenerationFinished
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.domain.quizrepo.RepoCheckout
import com.nexters.gitit.domain.quizrepo.RepoCoordinates
import com.nexters.gitit.infrastructure.github.GithubRepositoryFetcher
import com.nexters.gitit.infrastructure.mongo.MongoAuditingConfiguration
import com.nexters.gitit.infrastructure.mongo.MongoQuizGenerationStarter
import com.nexters.gitit.infrastructure.mongo.MongoQuizRepoRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import com.nexters.gitit.infrastructure.quiz.AnchorLocator
import com.nexters.gitit.infrastructure.quiz.DocumentAnalyzer
import com.nexters.gitit.infrastructure.quiz.QualityInspector
import com.nexters.gitit.infrastructure.quiz.QuestionGenerator
import com.nexters.gitit.infrastructure.time.ClockConfiguration
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 같은 저장소에 두 실행이 겹치면 LLM 콜이 두 배로 나가고, 뒤에 끝난 쪽이 앞의 결과를 덮습니다.
 * 그것을 막는 장치가 조건부 갱신 하나뿐이라 실제 MongoDB에 대고 확인합니다.
 *
 * 파이프라인 단계는 전부 목입니다 — 여기서 볼 것은 GitHub·Gemini가 아니라 몇 번 불렸는지입니다.
 */
@DataMongoTest
@Import(TestcontainersConfiguration::class, MongoAuditingConfiguration::class, ClockConfiguration::class)
class GenerateQuizConcurrencyTest(
    @Autowired private val mongoTemplate: MongoTemplate,
    @Autowired private val springDataQuizRepoRepository: SpringDataQuizRepoRepository,
) {
    private val githubRepositoryFetcher: GithubRepositoryFetcher = mock()
    private val documentAnalyzer: DocumentAnalyzer = mock()
    private val anchorLocator: AnchorLocator = mock()
    private val questionGenerator: QuestionGenerator = mock()
    private val qualityInspector: QualityInspector = mock()
    private val eventPublisher: ApplicationEventPublisher = mock()

    private val quizRepoRepository = MongoQuizRepoRepository(springDataQuizRepoRepository)

    private val quizGenerationStarter = MongoQuizGenerationStarter(mongoTemplate)

    private var now = Instant.parse("2026-08-18T00:00:00Z")

    // 시효를 실제로 기다릴 수는 없으므로 경과를 시계로 만든다.
    private val clock =
        object : Clock() {
            override fun instant(): Instant = now

            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId): Clock = this
        }

    private val anchor = Anchor("src/Router.kt", 10, 20, AnchorKind.DEFINITION, "class Router")

    private val concept = Concept("라우팅", "라우팅은 Router가 전담합니다.", "README.md", listOf("src/Router.kt"))

    private val checkout = RepoCheckout(ROOT, COORDINATES)

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

    @BeforeEach
    fun stubPipeline() {
        springDataQuizRepoRepository.deleteAll()
        now = Instant.parse("2026-08-18T00:00:00Z")

        val concepts = listOf(concept)
        val anchored = listOf(AnchoredConcept(concept, listOf(anchor)))
        whenever(documentAnalyzer.analyze(ROOT)) doReturn concepts
        whenever(anchorLocator.locate(ROOT, concepts)) doReturn anchored
        whenever(questionGenerator.generate(ROOT, anchored)) doReturn emptyList()
        whenever(qualityInspector.inspect(ROOT, emptyList())) doReturn emptyList()
    }

    @Test
    fun `같은 저장소로 동시에 들어와도 하나만 실행한다`() {
        val quizRepo = springDataQuizRepoRepository.save(quizRepoOf())
        whenever(githubRepositoryFetcher.fetch(REPO_URL)) doReturn checkout

        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            // 두 스레드를 같은 문에 세워 두고 한 번에 연다.
            val runs =
                (1..2).map {
                    executor.submit {
                        start.await()
                        generateQuiz(GenerateQuiz.Command(quizRepo.id))
                    }
                }
            start.countDown()
            runs.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        // 진 쪽은 선점에서 돌아섰으므로 첫 단계인 수집조차 부르지 않는다.
        verify(githubRepositoryFetcher, times(1)).fetch(any())
        springDataQuizRepoRepository
            .findById(quizRepo.id)
            .orElse(null)
            .shouldNotBeNull()
            .status shouldBe QuizRepoStatus.COMPLETED
    }

    @Test
    fun `이미 돌고 있는 저장소는 새 요청을 받지 않는다`() {
        val quizRepo = springDataQuizRepoRepository.save(quizRepoOf())
        // 점유는 도큐먼트에만 남는다. 남이 쥔 상태를 그대로 만들면 되고, 그 남이 실제로 돌고 있을 필요는 없다.
        quizGenerationStarter.start(quizRepo.id, Instant.now().plus(TIMEOUT))

        generateQuiz(GenerateQuiz.Command(quizRepo.id))

        verify(githubRepositoryFetcher, never()).fetch(any())
        springDataQuizRepoRepository
            .findById(quizRepo.id)
            .orElse(null)
            .shouldNotBeNull()
            .status shouldBe QuizRepoStatus.STARTED
    }

    @Test
    fun `시효가 지난 뒤 끝난 실행은 결과를 남기지 않는다`() {
        val quizRepo = springDataQuizRepoRepository.save(quizRepoOf())
        // 점유해 놓고 시효가 다 지나도록 멎었다 깨어난 실행이다. 그 사이 새 회차가 시작됐을 수 있다.
        whenever(githubRepositoryFetcher.fetch(REPO_URL)).then {
            now = now.plus(TIMEOUT).plusSeconds(1)
            checkout
        }

        generateQuiz(GenerateQuiz.Command(quizRepo.id))

        val found = springDataQuizRepoRepository.findById(quizRepo.id).orElse(null).shouldNotBeNull()
        // 문제를 다 만들고도 결말을 적지 않았다. COMPLETED로 적혔다면 새 회차의 결과를 되돌린 것이다.
        found.status shouldBe QuizRepoStatus.STARTED
        // 점유도 풀지 않았다. 이 실행은 자기 것이 아닌 점유를 건드릴 자격이 없다.
        found.timeoutAt shouldBe TIMEOUT_AT_OF_FIRST_RUN
        // 알리지도 않았다. 이 저장소는 회수되어 다시 도는데, 여기서 알리면 곧 성공할 생성을 실패로 알린다.
        verify(eventPublisher, never()).publishEvent(any<QuizGenerationFinished>())
    }

    // 표시용 필드는 선점 규약과 무관해 아무 값이나 채운다.
    private fun quizRepoOf() =
        QuizRepo(
            githubRepoId = "1310710749",
            githubRepoUrl = REPO_URL,
            name = "Git-it-Server",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
            registeredAt = Instant.EPOCH,
        )

    companion object {
        private const val REPO_URL = "https://github.com/Nexters/Git-it-Server"
        private val COORDINATES = RepoCoordinates("Nexters", "Git-it-Server", "abc1234")
        private val TIMEOUT = Duration.ofHours(1)
        private val TIMEOUT_AT_OF_FIRST_RUN = Instant.parse("2026-08-18T01:00:00Z")

        // 해제 디렉터리를 실제로 만들지 않는다. 파이프라인이 전부 목이라 이 경로를 읽는 코드가 없다.
        private val ROOT: Path = Path.of("/work/Nexters-Git-it-Server-abc1234")
    }
}
