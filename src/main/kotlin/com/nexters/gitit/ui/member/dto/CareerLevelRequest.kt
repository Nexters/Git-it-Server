package com.nexters.gitit.ui.member.dto

import com.nexters.gitit.application.UpdateMemberCareerLevel
import com.nexters.gitit.domain.member.CareerLevel
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class CareerLevelRequest(
    @field:Schema(description = "개발 수준")
    @field:NotNull(message = "careerLevel은 필수입니다")
    val careerLevel: CareerLevel?,
) {
    fun toCommand(memberId: String) =
        UpdateMemberCareerLevel.Command(
            memberId = memberId,
            careerLevel = careerLevel!!,
        )
}
