package com.nexters.gitit.application

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.QuizLevel
import com.nexters.gitit.domain.quizrepo.QuizGenerationRequested
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.infrastructure.mongo.SpringDataProjectRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@Import(TestcontainersConfiguration::class, RetryQuizGenerationTest.QuizGenerationRequestedCaptor::class)
class RetryQuizGenerationTest(
    @Autowired private val retryQuizGeneration: RetryQuizGeneration,
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
    @Autowired private val projectRepository: SpringDataProjectRepository,
    @Autowired private val eventCaptor: QuizGenerationRequestedCaptor,
) {
    // 이벤트가 나가면 진짜 파이프라인이 비동기로 돌아 GitHub와 Gemini를 부른다. 여기서 볼 것은 "다시 걸었는가"까지다.
    @MockitoBean
    private lateinit var generateQuiz: GenerateQuiz

    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        quizRepoRepository.deleteAll()
        projectRepository.deleteAll()
        eventCaptor.clear()
    }

    @Test
    fun `사고로 멈춘 저장소는 실패 직전 상태로 되돌리고 문제 생성을 다시 건다`() {
        val quizRepo =
            savedQuizRepo {
                checkpoint(SHA, emptyList())
                fail()
            }
        val project = projectRepository.save(Project("member-1", quizRepo.id, QuizLevel.L2))

        retryQuizGeneration(RetryQuizGeneration.Command("member-1", project.id))

        val retried = quizRepoRepository.findAll().single()
        // ANCHORED로 돌아와야 다시 도는 파이프라인이 앵커를 재계산하지 않는다.
        retried.status shouldBe QuizRepoStatus.ANCHORED
        retried.failedFrom shouldBe null
        retried.sha shouldBe SHA

        eventCaptor.received shouldBe listOf(QuizGenerationRequested(quizRepo.id))
    }

    @Test
    fun `문제를 낼 수 없다고 판정된 저장소는 재시도를 거절하고 생성을 걸지 않는다`() {
        val quizRepo = savedQuizRepo { reject(ErrorCode.NO_CONCEPTS) }
        val project = projectRepository.save(Project("member-1", quizRepo.id, QuizLevel.L2))

        val exception = shouldThrow<BaseException> { retryQuizGeneration(RetryQuizGeneration.Command("member-1", project.id)) }

        exception.errorCode shouldBe ErrorCode.QUIZ_GENERATION_NOT_RETRYABLE
        eventCaptor.received.shouldBeEmpty()
    }

    private fun savedQuizRepo(prepare: QuizRepo.() -> Unit) =
        quizRepoRepository.save(
            QuizRepo(
                githubRepoId = "7517918",
                githubRepoUrl = "https://github.com/spring-projects/spring-petclinic",
                name = "spring-petclinic",
                ownerImageUrl = "https://avatars.githubusercontent.com/u/317776?v=4",
                starCount = 7800,
                techStacks = listOf("java"),
            ).apply(prepare),
        )

    /**
     * 이벤트가 실제로 나갔는지만 봅니다. 퍼블리셔를 목으로 바꾸면 컨텍스트 전체의 이벤트가 죽어 다른 검증까지 흔들립니다.
     */
    class QuizGenerationRequestedCaptor {
        val received = mutableListOf<QuizGenerationRequested>()

        @EventListener
        fun capture(event: QuizGenerationRequested) {
            received += event
        }

        fun clear() = received.clear()
    }

    companion object {
        private const val SHA = "abc1234"
    }
}
