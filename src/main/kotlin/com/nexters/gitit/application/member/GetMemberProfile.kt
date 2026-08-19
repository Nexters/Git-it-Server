package com.nexters.gitit.application.member

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.member.CareerLevel
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.member.Position
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class GetMemberProfile(
    private val memberRepository: MemberRepository,
    private val projectRepository: ProjectRepository,
    private val clock: Clock,
) {
    operator fun invoke(command: Command): Result {
        val member = memberRepository.findById(command.memberId) ?: throw BaseException(ErrorCode.MEMBER_NOT_FOUND)
        val solvedDates =
            projectRepository
                .findAllByMemberId(command.memberId)
                .flatMap { it.answers }
                .map { it.answeredAt.atZone(clock.zone).toLocalDate() }

        val today = LocalDate.now(clock)
        val weekStart = today.with(DayOfWeek.MONDAY)
        val monthStart = today.withDayOfMonth(1)

        return Result(
            name = member.name,
            email = member.email,
            position = member.position,
            careerLevel = member.careerLevel,
            thisWeekSolvedCount = solvedDates.count { it >= weekStart },
            thisMonthSolvedCount = solvedDates.count { it >= monthStart },
            streakDays = streakDays(solvedDates.toSet(), today),
            weeklyChart = weeklyChart(solvedDates, weekStart),
        )
    }

    /** 오늘 아직 안 풀었어도 어제까지 이어져 있으면 스트릭이 유지된 것으로 봅니다. */
    private fun streakDays(
        activeDates: Set<LocalDate>,
        today: LocalDate,
    ): Int {
        var day = if (today in activeDates) today else today.minusDays(1)
        var streak = 0
        while (day in activeDates) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    private fun weeklyChart(
        solvedDates: List<LocalDate>,
        weekStart: LocalDate,
    ): List<DayCount> {
        val countByDate = solvedDates.groupingBy { it }.eachCount()
        return DAY_LABELS.mapIndexed { offset, label ->
            DayCount(dayLabel = label, count = countByDate[weekStart.plusDays(offset.toLong())] ?: 0)
        }
    }

    data class Command(
        val memberId: String,
    )

    data class Result(
        val name: String,
        val email: String?,
        val position: Position?,
        val careerLevel: CareerLevel?,
        val thisWeekSolvedCount: Int,
        val thisMonthSolvedCount: Int,
        val streakDays: Int,
        val weeklyChart: List<DayCount>,
    )

    data class DayCount(
        val dayLabel: String,
        val count: Int,
    )

    companion object {
        private val DAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")
    }
}
