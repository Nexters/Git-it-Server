package com.nexters.gitit.infrastructure.quiz

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ParallelTest {
    @Test
    fun `일부가 실패해도 성공한 결과는 살아남는다`() {
        val outcomes = listOf(1, 2, 3, 4).inParallel { runCatching { if (it % 2 == 0) error("$it 실패") else "값 $it" } }

        // 죽은 것 때문에 이미 성공한 콜까지 버리지 않는 것이 이 함수의 전부다.
        outcomes.successesOrThrow() shouldBe listOf("값 1", "값 3")
    }

    @Test
    fun `전부 실패하면 원래 예외를 그대로 던진다`() {
        val outcomes = listOf(1, 2).inParallel { runCatching { error("$it 실패") } }

        // 삼키면 뒷단계가 빈 결과를 "재료가 없다"고 읽어, 사고가 되돌릴 수 없는 거절로 굳는다.
        val exception = shouldThrow<IllegalStateException> { outcomes.successesOrThrow() }

        exception.message shouldBe "1 실패"
    }
}
