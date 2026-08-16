package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.SubmitAnswer
import io.swagger.v3.oas.annotations.media.Schema

/**
 * selectedIndex에는 범위 검증을 붙이지 않습니다. 선택지 개수는 문제마다 정해지는 값이라 요청만 보고는
 * 상한을 알 수 없고, 실제 판정은 문제 자신이 합니다.
 */
data class SubmitChoiceAnswerRequest(
    @field:Schema(description = "고른 선택지 번호. 문제의 choices 순서를 따르는 0부터 시작하는 번호입니다", example = "2")
    val selectedIndex: Int,
) {
    fun toCommand(
        memberId: String,
        projectId: String,
        questionId: String,
    ) = SubmitAnswer.Command.Choice(
        memberId = memberId,
        projectId = projectId,
        questionId = questionId,
        selectedIndex = selectedIndex,
    )
}
