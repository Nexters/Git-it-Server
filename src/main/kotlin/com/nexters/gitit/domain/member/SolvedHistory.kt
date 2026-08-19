package com.nexters.gitit.domain.member

import java.time.LocalDate

/**
 * 회원이 문제를 푼 날짜들입니다. 같은 날 여러 문제를 풀면 그날이 푼 수만큼 들어 있습니다.
 *
 * 여러 프로젝트의 답안을 합친 값이라 프로젝트 하나로는 만들 수 없고, 어느 날 어떤 문제를 풀었는지는 잊습니다.
 */
@JvmInline
value class SolvedHistory(
    private val solvedDates: List<LocalDate>,
) {
    fun countSince(date: LocalDate): Int = solvedDates.count { it >= date }

    /** 오늘 아직 안 풀었어도 어제까지 이어져 있으면 스트릭이 유지된 것으로 봅니다. */
    fun streakDays(today: LocalDate): Int {
        val activeDates = solvedDates.toSet()
        var day = if (today in activeDates) today else today.minusDays(1)
        var streak = 0
        while (day in activeDates) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    /** [weekStart]부터 이레 동안 하루씩 푼 개수입니다. 안 푼 날은 0입니다. */
    fun weeklyCounts(weekStart: LocalDate): List<Int> {
        val countByDate = solvedDates.groupingBy { it }.eachCount()
        return (0 until DAYS_IN_WEEK).map { countByDate[weekStart.plusDays(it.toLong())] ?: 0 }
    }

    companion object {
        private const val DAYS_IN_WEEK = 7
    }
}
