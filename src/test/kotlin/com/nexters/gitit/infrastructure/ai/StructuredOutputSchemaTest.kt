package com.nexters.gitit.infrastructure.ai

import com.nexters.gitit.infrastructure.quiz.LearningSetDraft
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.ai.converter.BeanOutputConverter

/**
 * 구조화 출력 스키마가 Gemini가 받아주는 모양인지 봅니다.
 *
 * Gemini의 responseSchema는 OpenAPI 3 서브셋이라 `${'$'}ref`·`${'$'}defs`를 거부합니다. 스키마 생성기는
 * 같은 타입이 두 번 이상 참조되면 정의를 빼내 참조로 바꾸므로, 중첩 타입을 공유하는 순간
 * 콜이 400으로 죽습니다. 그때까지 컴파일도 테스트도 통과하기 때문에 여기서 잡습니다.
 */
class StructuredOutputSchemaTest {
    @Test
    fun `스키마가 참조 없이 펼쳐진다`() {
        // 가장 깊은 것 하나만 본다. 다른 단계는 이보다 얕아 여기가 통과하면 같이 통과한다.
        val schema = BeanOutputConverter(LearningSetDraft::class.java).jsonSchema

        schema shouldNotContain "\$ref"
        schema shouldNotContain "\$defs"
    }
}
