package com.nexters.gitit.application

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.Question
import com.nexters.gitit.domain.quizrepo.QuizRepo

/**
 * 문제는 세트 순서 → 세트 내 문제 순서로 이어 붙인 하나의 순번으로 풀린다고 가정합니다.
 * [Project.solvedQuestionCount](지금까지 푼 문제 수)만 있으면 그 순번을 진행 위치로 쓸 수 있어
 * 회원별 정답 이력을 별도로 저장하지 않아도 진행률/다음 문제/세트별 완료 개수를 계산할 수 있습니다.
 */
object ProjectProgress {
    fun calculate(
        project: Project,
        quizRepo: QuizRepo,
    ): Result {
        val depth = Depth.valueOf(project.quizLevel.name)
        val questionsPerSet = quizRepo.learningSets.map { it.questions[depth].orEmpty() }
        val totalCount = questionsPerSet.sumOf { it.size }

        if (totalCount == 0) {
            return Result(
                overallProgressPercent = 0,
                nextQuestionId = null,
                nextSetIndex = null,
                completedCountsBySet = questionsPerSet.map { 0 },
            )
        }

        val solvedCount = project.solvedQuestionCount.coerceAtMost(totalCount)
        val overallProgressPercent = solvedCount * 100 / totalCount

        // 다 풀었으면(혹은 애초에 하나도 안 풀었으면) 다음 문제는 1세트 1번으로 돌아간다.
        // 진행률 바에 쓰는 실제 완료 개수(solvedCount)와는 별개로 다룬다.
        val nextPosition = if (project.solvedQuestionCount >= totalCount) 0 else project.solvedQuestionCount
        val (nextSetIndex, nextQuestionId) = locate(questionsPerSet, nextPosition)

        return Result(
            overallProgressPercent = overallProgressPercent,
            nextQuestionId = nextQuestionId,
            nextSetIndex = nextSetIndex,
            completedCountsBySet = distribute(questionsPerSet, solvedCount),
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

    private fun distribute(
        questionsPerSet: List<List<Question>>,
        solvedCount: Int,
    ): List<Int> {
        var remaining = solvedCount
        return questionsPerSet.map { questions ->
            val completed = remaining.coerceAtMost(questions.size)
            remaining -= completed
            completed
        }
    }

    data class Result(
        val overallProgressPercent: Int,
        val nextQuestionId: String?,
        val nextSetIndex: Int?,
        val completedCountsBySet: List<Int>,
    )
}

/** GitHub URL만 저장돼 있어서 이름은 읽을 때마다 마지막 경로 조각으로 만듭니다. */
fun repositoryNameOf(githubRepoUrl: String): String = githubRepoUrl.trimEnd('/').substringAfterLast('/')
