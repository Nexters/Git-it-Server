package com.nexters.gitit.application.project

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.QuizLevel
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.Question
import com.nexters.gitit.domain.quizrepo.QuizRepo

/**
 * 진행률/다음 문제/세트별 완료 개수를 [Project.answers]로 계산합니다.
 *
 * "다음 문제"는 세트 순서 → 세트 내 문제 순서로 이어 붙였을 때 답이 없는 첫 문제입니다.
 * 이미 낸 답이 있는지만 보므로, 문제를 어떤 순서로 풀었는지는 결과에 영향을 주지 않습니다.
 */
object ProjectProgress {
    fun calculate(
        project: Project,
        quizRepo: QuizRepo,
    ): Result {
        val depth = project.quizLevel.toDepth()
        val questionsPerSet = quizRepo.learningSets.map { it.questionsOf(depth) }
        val totalCount = questionsPerSet.sumOf { it.size }

        if (totalCount == 0) {
            return Result(
                overallProgressPercent = 0,
                nextQuestionId = null,
                nextSetIndex = null,
                completedCountsBySet = questionsPerSet.map { 0 },
            )
        }

        val answeredIds = project.answers.mapTo(HashSet()) { it.questionId }
        val flat = questionsPerSet.flatten()
        val solvedCount = flat.count { it.id in answeredIds }
        val overallProgressPercent = solvedCount * 100 / totalCount

        // 다 풀었으면(혹은 애초에 하나도 안 풀었으면) 다음 문제는 1세트 1번으로 돌아간다.
        // 진행률 바에 쓰는 실제 완료 개수(solvedCount)와는 별개로 다룬다.
        val nextPosition = flat.indexOfFirst { it.id !in answeredIds }.let { if (it == -1) 0 else it }
        val (nextSetIndex, nextQuestionId) = locate(questionsPerSet, nextPosition)

        return Result(
            overallProgressPercent = overallProgressPercent,
            nextQuestionId = nextQuestionId,
            nextSetIndex = nextSetIndex,
            completedCountsBySet = questionsPerSet.map { questions -> questions.count { it.id in answeredIds } },
        )
    }

    private fun locate(
        questionsPerSet: List<List<Question>>,
        position: Int,
    ): Pair<Int?, String?> {
        var remaining = position
        for ((index, questions) in questionsPerSet.withIndex()) {
            if (remaining < questions.size) return index to questions[remaining].id
            remaining -= questions.size
        }
        return null to null
    }

    // 이름이 같아도 enum이 둘이라 valueOf로 잇지 않는다. 한쪽에 레벨이 늘면 컴파일이 깨져야 옮겨 적는 것을 잊지 않는다.
    private fun QuizLevel.toDepth() =
        when (this) {
            QuizLevel.L1 -> Depth.L1
            QuizLevel.L2 -> Depth.L2
            QuizLevel.L3 -> Depth.L3
        }

    data class Result(
        val overallProgressPercent: Int,
        val nextQuestionId: String?,
        val nextSetIndex: Int?,
        val completedCountsBySet: List<Int>,
    )
}
