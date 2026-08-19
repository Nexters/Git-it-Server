package com.nexters.gitit.application.member

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.member.CareerLevel
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.member.Position
import com.nexters.gitit.domain.member.SolvedHistory
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
        val history =
            SolvedHistory(
                projectRepository
                    .findAllByMemberId(command.memberId)
                    .flatMap { it.answers }
                    .map { it.answeredAt.atZone(clock.zone).toLocalDate() },
            )

        val today = LocalDate.now(clock)
        val weekStart = today.with(DayOfWeek.MONDAY)
        val monthStart = today.withDayOfMonth(1)

        return Result(
            name = member.name,
            email = member.email,
            position = member.position,
            careerLevel = member.careerLevel,
            thisWeekSolvedCount = history.countSince(weekStart),
            thisMonthSolvedCount = history.countSince(monthStart),
            streakDays = history.streakDays(today),
            weeklyCounts = history.weeklyCounts(weekStart),
        )
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
        /** 이번 주 월요일부터 일요일까지 요일별 푼 개수 7개. */
        val weeklyCounts: List<Int>,
    )
}
