package com.nexters.gitit.domain.project

/**
 * 한 프로젝트의 학습 진행 상태입니다. [ProjectProgressCalculator]가 만듭니다.
 *
 * 문제가 하나도 없으면 [nextQuestionId]와 [nextSetIndex]가 null입니다.
 */
data class ProjectProgress(
    val overallProgressPercent: Int,
    val nextQuestionId: String?,
    val nextSetIndex: Int?,
    val completedCountsBySet: List<Int>,
)
