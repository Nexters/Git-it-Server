package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.SubmitAnswer
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class SubmitEssayAnswerRequest(
    @field:Schema(description = "서술형 답안. 채점은 채점 기준을 보고 학습자가 스스로 합니다. 비워서 낼 수 있습니다", example = "경로가 Router 한 곳에 모여 있어 요청 흐름을 따라가기 쉽습니다")
    @field:Size(max = MAX_TEXT_LENGTH, message = "text는 ${MAX_TEXT_LENGTH}자를 넘을 수 없습니다")
    val text: String,
) {
    fun toCommand(
        memberId: String,
        projectId: String,
        questionId: String,
    ) = SubmitAnswer.Command.Essay(
        memberId = memberId,
        projectId = projectId,
        questionId = questionId,
        text = text,
    )

    companion object {
        // 답안 한 편의 분량으로 넉넉한 상한. 도큐먼트에 임베드되므로 상한 없이 받으면 프로젝트 하나가 무한정 커진다.
        const val MAX_TEXT_LENGTH = 2000
    }
}
