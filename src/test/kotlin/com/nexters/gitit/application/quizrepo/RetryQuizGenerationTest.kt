package com.nexters.gitit.application.quizrepo

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.domain.quizrepo.failed
import com.nexters.gitit.domain.quizrepo.rejected
import com.nexters.gitit.infrastructure.mongo.SpringDataProjectRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Instant

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class RetryQuizGenerationTest(
    @Autowired private val retryQuizGeneration: RetryQuizGeneration,
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
    @Autowired private val projectRepository: SpringDataProjectRepository,
) {
    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        quizRepoRepository.deleteAll()
        projectRepository.deleteAll()
    }

    @Test
    fun `사고로 멈춘 저장소는 대기줄 맨 뒤에 다시 세운다`() {
        val quizRepo =
            savedQuizRepo {
                checkpoint(SHA, emptyList())
                failed()
            }
        val project = projectRepository.save(Project("member-1", quizRepo.id, Depth.L2))

        retryQuizGeneration(RetryQuizGeneration.Command("member-1", project.id))

        val retried = quizRepoRepository.findAll().single()
        // 대기줄이 READY 하나라 여기로 돌아와야 스케줄러가 집어 간다.
        retried.status shouldBe QuizRepoStatus.READY
        // 앵커를 재계산하지 않는 근거는 상태가 아니라 이 값이다.
        retried.sha shouldBe SHA
        // 점유는 결말과 함께 풀렸다. 남아 있으면 다시 집힌 회차가 시효 검사에서 남의 점유를 본다.
        retried.timeoutAt shouldBe null
        // 줄 맨 뒤로 다시 섰다.
        retried.registeredAt shouldBeGreaterThan Instant.EPOCH
    }

    @Test
    fun `문제를 낼 수 없다고 판정된 저장소는 재시도를 거절한다`() {
        val quizRepo =
            savedQuizRepo { rejected(ErrorCode.NO_CONCEPTS) }
        val project = projectRepository.save(Project("member-1", quizRepo.id, Depth.L2))

        val exception = shouldThrow<BaseException> { retryQuizGeneration(RetryQuizGeneration.Command("member-1", project.id)) }

        exception.errorCode shouldBe ErrorCode.QUIZ_GENERATION_NOT_RETRYABLE
        quizRepoRepository.findAll().single().status shouldBe QuizRepoStatus.REJECTED
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
                registeredAt = Instant.EPOCH,
            ).apply(prepare),
        )

    companion object {
        private const val SHA = "abc1234"
    }
}
