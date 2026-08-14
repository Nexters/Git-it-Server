package com.nexters.gitit.infrastructure.ai

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.infrastructure.quiz.ConceptCandidate
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 개념 추출 콜의 Gemini 구현.
 *
 * 개념이 하나도 안 나온 응답은 오류로 보지 않습니다 — 재료가 부실한 레포에서는 정상적인 결과이고,
 * 다시 물어도 같은 답이 옵니다. 그 판정은 [com.nexters.gitit.infrastructure.quiz.ConceptGate]가 합니다.
 */
@Component
class GeminiConceptExtractor(
    builder: ChatClient.Builder,
    @Value("\${ai.model.concept}") model: String,
) : ConceptExtractor {
    private val call = StructuredChatCall(builder, "Concept extraction", model, SYSTEM_PROMPT, ConceptExtractionResponse::class.java)

    override fun extract(
        bundle: String,
        sourceTree: String,
    ): List<ConceptCandidate> {
        val userText =
            """
            |# 문서
            |
            |$bundle
            |
            |# 소스 파일 목록
            |
            |$sourceTree
            """.trimMargin()

        return call.call(userText)?.concepts
            ?: throw BaseException(ErrorCode.CONCEPT_EXTRACTION_FAILED, "개념 추출 응답을 해석하지 못했습니다")
    }

    /** 구조화 출력 스키마. 스키마로 형식을 강제하면 모든 필드가 required가 되므로 nullable을 두지 않습니다. */
    data class ConceptExtractionResponse(
        val concepts: List<ConceptCandidate>,
    )

    companion object {
        private val SYSTEM_PROMPT = ClassPathResource("prompts/concept-extraction.st")
    }
}
