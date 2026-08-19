package com.nexters.gitit.domain.project

import com.nexters.gitit.domain.quizrepo.Depth
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.Instant

class ProjectTest {
    @Test
    fun `다른 문제의 답은 함께 남고 같은 문제의 답만 최신 것으로 갈린다`() {
        val project = Project(memberId = "member-1", quizRepoId = "quiz-repo-1", quizLevel = Depth.L2)
        project.submit(choiceOf("question-1", selectedIndex = 0, correct = false))
        project.submit(choiceOf("question-2", selectedIndex = 3, correct = true))

        project.submit(choiceOf("question-1", selectedIndex = 2, correct = true))

        project.answers.size shouldBe 2
        val answer = project.answers.single { it.questionId == "question-1" }.shouldBeInstanceOf<Answer.Choice>()
        answer.selectedIndex shouldBe 2
        answer.correct shouldBe true
    }

    private fun choiceOf(
        questionId: String,
        selectedIndex: Int,
        correct: Boolean,
    ) = Answer.Choice(
        questionId = questionId,
        answeredAt = Instant.parse("2026-08-16T00:00:00Z"),
        selectedIndex = selectedIndex,
        correct = correct,
    )
}
