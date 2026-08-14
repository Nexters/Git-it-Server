package com.nexters.gitit.infrastructure.ai

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.quiz.AnchorCandidate
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 앵커 선택 콜의 Gemini 구현.
 *
 * 개념 이름과 문서 근거를 함께 보냅니다. 근거 없이 코드만 주면 "이 파일에서 중요해 보이는 곳"을
 * 고르게 되어, 개념과 무관한 진입점·설정 코드가 앵커로 올라옵니다.
 */
@Component
class GeminiAnchorSelector(
    builder: ChatClient.Builder,
    @Value("\${ai.model.anchor}") model: String,
) : AnchorSelector {
    private val call = StructuredChatCall(builder, "Anchor selection", model, SYSTEM_PROMPT, AnchorSelectionResponse::class.java)

    override fun select(
        concept: Concept,
        bundle: String,
    ): List<AnchorCandidate> {
        val userText =
            """
            |개념: ${concept.name}
            |문서 근거: ${concept.rationale}
            |
            |$bundle
            """.trimMargin()

        return call.call(userText)?.anchors
            ?: throw BaseException(ErrorCode.ANCHOR_SELECTION_FAILED, "앵커 선택 응답을 해석하지 못했습니다")
    }

    data class AnchorSelectionResponse(
        val anchors: List<AnchorCandidate>,
    )

    companion object {
        private val SYSTEM_PROMPT = ClassPathResource("prompts/anchor-selection.st")
    }
}
