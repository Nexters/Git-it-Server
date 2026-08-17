package com.nexters.gitit.domain.quizrepo

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class QuizRepoTest {
    @Test
    fun `경로에 예약 문자가 섞여 있어도 열리는 GitHub 주소를 만든다`() {
        val quizRepo = quizRepoOf().apply { complete("abc1234", emptyList()) }

        val url = quizRepo.sourceUrlOf(Anchor("src/main/my note#1.kt", 10, 20, AnchorKind.DEFINITION, "class Router"))

        url shouldBe "https://github.com/Nexters/Git-it-Server/blob/abc1234/src/main/my%20note%231.kt#L10-L20"
    }

    @Test
    fun `재시도는 대기줄 맨 뒤에 다시 세우되, 만들어 둔 앵커의 표식은 남긴다`() {
        val anchored = listOf(AnchoredConcept(Concept("라우팅", "라우팅은 Router가 전담합니다.", "README.md", emptyList()), emptyList()))
        val quizRepo =
            quizRepoOf().apply {
                checkpoint("abc1234", anchored)
                fail()
            }

        quizRepo.retry(Clock.fixed(RETRIED_AT, ZoneOffset.UTC))

        // 대기줄이 READY 하나라 여기로 돌아와야 집힌다.
        quizRepo.status shouldBe QuizRepoStatus.READY
        // 어느 콜 범위에서 돈이 멈췄는지는 상태가 READY로 돌아온 뒤에도 남는다.
        quizRepo.failedFrom shouldBe QuizRepoStatus.ANCHORED
        // 앵커를 다시 만들지 않는 근거는 이 값이다.
        quizRepo.anchoredConcepts shouldBe anchored
        quizRepo.registeredAt shouldBe RETRIED_AT
    }

    @Test
    fun `사고로 멈춘 것이 아니면 재시도를 거절한다`() {
        val quizRepo = quizRepoOf().apply { reject(ErrorCode.NO_CONCEPTS) }

        val exception = shouldThrow<BaseException> { quizRepo.retry(Clock.systemUTC()) }

        exception.errorCode shouldBe ErrorCode.QUIZ_GENERATION_NOT_RETRYABLE
        quizRepo.status shouldBe QuizRepoStatus.REJECTED
    }

    private fun quizRepoOf() =
        QuizRepo(
            githubRepoId = "1310710749",
            githubRepoUrl = "https://github.com/Nexters/Git-it-Server",
            name = "Git-it-Server",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
            registeredAt = Instant.EPOCH,
        )

    companion object {
        private val RETRIED_AT = Instant.parse("2026-08-17T00:00:00Z")
    }
}
