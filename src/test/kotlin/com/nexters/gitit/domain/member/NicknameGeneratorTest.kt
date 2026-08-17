package com.nexters.gitit.domain.member

import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.random.Random

class NicknameGeneratorTest {
    private val nicknameGenerator = NicknameGenerator()

    @Test
    fun `같은 시드면 같은 닉네임을 만든다`() {
        val first = nicknameGenerator.generate(Random(42))
        val second = nicknameGenerator.generate(Random(42))

        first shouldBe second
    }

    // 상한을 뽑을 때가 아니라 단어를 고를 때 지키므로, 목록에 긴 단어가 들어오면 여기서만 걸린다.
    @Test
    fun `어떤 조합이 나와도 15자를 넘지 않는다`() {
        for (adjective in nicknameGenerator.adjectives) {
            for (techTerm in nicknameGenerator.techTerms) {
                "$adjective $techTerm".length shouldBeLessThanOrEqualTo 15
            }
        }
    }
}
