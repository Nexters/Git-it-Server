package com.nexters.gitit.ui.member.dto

import com.nexters.gitit.application.GetMemberProfile
import com.nexters.gitit.domain.member.CareerLevel
import com.nexters.gitit.domain.member.Position
import io.swagger.v3.oas.annotations.media.Schema

data class MemberProfileResponse(
    val email: String?,
    @field:Schema(description = "개발 분야")
    val position: Position?,
    @field:Schema(description = "개발 수준")
    val careerLevel: CareerLevel?,
    @field:Schema(description = "세트 생성 완료 알림 수신 여부")
    val setCompletionReminderEnabled: Boolean,
    @field:Schema(description = "이번 주(월요일부터 오늘까지) 푼 문제 수")
    val thisWeekSolvedCount: Int,
    @field:Schema(description = "이번 달(1일부터 오늘까지) 푼 문제 수")
    val thisMonthSolvedCount: Int,
    @field:Schema(description = "연속 학습 일수")
    val streakDays: Int,
    @field:Schema(description = "이번 주 요일별 문제 풀이량 (월요일부터 일요일까지 7개)")
    val weeklyChart: List<DayCountResponse>,
) {
    companion object {
        fun from(result: GetMemberProfile.Result) =
            MemberProfileResponse(
                email = result.email,
                position = result.position,
                careerLevel = result.careerLevel,
                setCompletionReminderEnabled = result.setCompletionReminderEnabled,
                thisWeekSolvedCount = result.thisWeekSolvedCount,
                thisMonthSolvedCount = result.thisMonthSolvedCount,
                streakDays = result.streakDays,
                weeklyChart = result.weeklyChart.map { DayCountResponse(it.dayLabel, it.count) },
            )
    }
}

data class DayCountResponse(
    val dayLabel: String,
    val count: Int,
)
