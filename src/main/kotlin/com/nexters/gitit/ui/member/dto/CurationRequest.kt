package com.nexters.gitit.ui.member.dto

import com.nexters.gitit.application.CurateMember
import com.nexters.gitit.domain.member.CareerLevel
import com.nexters.gitit.domain.member.Position
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class CurationRequest(
    @field:Schema(description = "관심 분야")
    @field:NotNull(message = "position은 필수입니다")
    val position: Position?,
    @field:Schema(description = "실력 수준")
    @field:NotNull(message = "careerLevel은 필수입니다")
    val careerLevel: CareerLevel?,
) {
    fun toCommand(memberId: String) =
        CurateMember.Command(
            memberId = memberId,
            position = position!!,
            careerLevel = careerLevel!!,
        )
}
