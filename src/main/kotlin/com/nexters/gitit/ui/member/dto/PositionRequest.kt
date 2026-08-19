package com.nexters.gitit.ui.member.dto

import com.nexters.gitit.application.member.UpdateMemberPosition
import com.nexters.gitit.domain.member.Position
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class PositionRequest(
    @field:Schema(description = "개발 분야")
    @field:NotNull(message = "position은 필수입니다")
    val position: Position?,
) {
    fun toCommand(memberId: String) =
        UpdateMemberPosition.Command(
            memberId = memberId,
            position = position!!,
        )
}
