package com.nexters.gitit.domain.quizrepo

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class QuizRepoTest {
    @Test
    fun `경로에 예약 문자가 섞여 있어도 열리는 GitHub 주소를 만든다`() {
        val quizRepo = quizRepoOf().apply { complete("abc1234", emptyList()) }

        val url = quizRepo.sourceUrlOf(Anchor("src/main/my note#1.kt", 10, 20, AnchorKind.DEFINITION, "class Router"))

        url shouldBe "https://github.com/Nexters/Git-it-Server/blob/abc1234/src/main/my%20note%231.kt#L10-L20"
    }

    private fun quizRepoOf() =
        QuizRepo(
            githubRepoId = "1310710749",
            githubRepoUrl = "https://github.com/Nexters/Git-it-Server",
            name = "Git-it-Server",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
        )
}
