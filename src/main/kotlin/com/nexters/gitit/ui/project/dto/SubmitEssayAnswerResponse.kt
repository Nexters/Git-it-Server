package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.project.SubmitAnswer
import com.nexters.gitit.domain.quizrepo.Rubric
import com.nexters.gitit.domain.quizrepo.RubricCriterion
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 서술형에는 정답 여부가 없습니다. 채점자가 학습자 자신이라, 정답 대신 스스로 대조할 채점 기준이 나갑니다.
 */
data class SubmitEssayAnswerResponse(
    @field:Schema(description = "답을 낸 문제 id")
    val questionId: String,
    @field:Schema(description = "해설")
    val explanation: String,
    @field:Schema(description = "자가채점 기준")
    val rubric: RubricResponse,
) {
    companion object {
        fun from(result: SubmitAnswer.Result.Essay) =
            SubmitEssayAnswerResponse(
                questionId = result.questionId,
                explanation = result.explanation,
                rubric = RubricResponse.from(result.rubric),
            )
    }
}

data class RubricResponse(
    @field:Schema(description = "판단 기준별 배점. 합이 만점입니다")
    val criteria: List<RubricCriterionResponse>,
    @field:Schema(description = "답안에 들어가야 할 핵심")
    val keyPoints: List<String>,
    @field:Schema(description = "만점 답안 예시")
    val fullMarkExample: String,
    @field:Schema(description = "부분 점수 답안 예시")
    val partialExample: String,
    @field:Schema(description = "0점 답안 예시")
    val zeroExample: String,
) {
    companion object {
        fun from(rubric: Rubric) =
            RubricResponse(
                criteria = rubric.criteria.map(RubricCriterionResponse::from),
                keyPoints = rubric.keyPoints,
                fullMarkExample = rubric.fullMarkExample,
                partialExample = rubric.partialExample,
                zeroExample = rubric.zeroExample,
            )
    }
}

data class RubricCriterionResponse(
    @field:Schema(description = "정답 나열이 아니라 판단 기준", example = "실제 파일명을 들어 설명했는가")
    val text: String,
    @field:Schema(description = "이 기준의 배점", example = "3")
    val points: Int,
) {
    companion object {
        fun from(criterion: RubricCriterion) = RubricCriterionResponse(criterion.text, criterion.points)
    }
}
