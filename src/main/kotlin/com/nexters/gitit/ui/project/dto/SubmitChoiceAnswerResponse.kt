package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.SubmitAnswer
import io.swagger.v3.oas.annotations.media.Schema

data class SubmitChoiceAnswerResponse(
    @field:Schema(description = "답을 낸 문제 id")
    val questionId: String,
    @field:Schema(description = "고른 선택지가 정답인지", example = "true")
    val correct: Boolean,
    @field:Schema(description = "정답 선택지 번호. 틀렸을 때 어느 것이 정답이었는지 바로 보여줄 수 있습니다", example = "2")
    val answerIndex: Int,
    @field:Schema(description = "해설. 선택지를 번호가 아니라 내용으로 가리킵니다")
    val explanation: String,
) {
    companion object {
        fun from(result: SubmitAnswer.Result.Choice) =
            SubmitChoiceAnswerResponse(
                questionId = result.questionId,
                correct = result.correct,
                answerIndex = result.answerIndex,
                explanation = result.explanation,
            )
    }
}
