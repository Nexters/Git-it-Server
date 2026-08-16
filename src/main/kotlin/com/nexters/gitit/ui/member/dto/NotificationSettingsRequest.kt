package com.nexters.gitit.ui.member.dto

import com.nexters.gitit.application.UpdateNotificationSettings
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class NotificationSettingsRequest(
    @field:Schema(description = "세트 생성 완료 알림 수신 여부")
    @field:NotNull(message = "setCompletionReminderEnabled는 필수입니다")
    val setCompletionReminderEnabled: Boolean?,
) {
    fun toCommand(memberId: String) =
        UpdateNotificationSettings.Command(
            memberId = memberId,
            setCompletionReminderEnabled = setCompletionReminderEnabled!!,
        )
}
