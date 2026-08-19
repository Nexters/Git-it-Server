package com.nexters.gitit.domain.member

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SolvedHistoryTest {
    @Test
    fun `기준 날짜부터 오늘까지 푼 개수를 센다`() {
        val history =
            SolvedHistory(
                listOf(
                    LocalDate.of(2026, 8, 16),
                    LocalDate.of(2026, 8, 17),
                    LocalDate.of(2026, 8, 17),
                    LocalDate.of(2026, 8, 19),
                ),
            )

        history.countSince(LocalDate.of(2026, 8, 17)) shouldBe 3
    }

    @Test
    fun `오늘 아직 안 풀었어도 어제까지 이어져 있으면 스트릭이 유지된다`() {
        val history =
            SolvedHistory(
                listOf(
                    LocalDate.of(2026, 8, 17),
                    LocalDate.of(2026, 8, 18),
                ),
            )

        history.streakDays(LocalDate.of(2026, 8, 19)) shouldBe 2
    }

    @Test
    fun `요일별 푼 개수를 월요일부터 이레치 낸다`() {
        val history =
            SolvedHistory(
                listOf(
                    LocalDate.of(2026, 8, 17),
                    LocalDate.of(2026, 8, 19),
                    LocalDate.of(2026, 8, 19),
                ),
            )

        history.weeklyCounts(LocalDate.of(2026, 8, 17)) shouldBe listOf(1, 0, 2, 0, 0, 0, 0)
    }
}
