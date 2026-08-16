package com.nexters.gitit.application

import com.nexters.gitit.domain.problem.Problem
import com.nexters.gitit.domain.problem.ProblemRepository
import org.springframework.stereotype.Service

/**
 * 프로젝트 목록/상세 조회에서 공통으로 쓰는 "전체 진행률 + 다음 문제" 계산.
 * 다음 문제는 가장 최근에 정답 제출한 문제(순번 기준 +1)이며, 다 풀었는지와 무관하게
 * 순번대로 이동한다. 마지막 문제 다음은 1번으로 되돌아간다(wrap-around).
 */
@Service
class ProjectProgressCalculator(
    private val problemRepository: ProblemRepository,
) {
    fun calculate(projectId: String): Result {
        val totalCount = problemRepository.countByProjectId(projectId)
        val answeredCount = problemRepository.countByProjectIdAndAnsweredAtIsNotNull(projectId)
        val overallProgressPercent = if (totalCount == 0L) 0 else (answeredCount * 100 / totalCount).toInt()

        val lastAnswered = problemRepository.findFirstByProjectIdAndAnsweredAtIsNotNullOrderByAnsweredAtDesc(projectId)
        val nextOrder =
            when {
                lastAnswered == null -> 1
                lastAnswered.order >= totalCount -> 1
                else -> lastAnswered.order + 1
            }
        val nextProblem = problemRepository.findFirstByProjectIdAndOrder(projectId, nextOrder)

        return Result(overallProgressPercent, nextProblem)
    }

    data class Result(
        val overallProgressPercent: Int,
        val nextProblem: Problem?,
    )
}
