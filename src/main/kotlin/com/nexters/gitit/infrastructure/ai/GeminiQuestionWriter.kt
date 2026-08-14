package com.nexters.gitit.infrastructure.ai

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.infrastructure.quiz.LearningSetDraft
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 생성 콜의 Gemini 구현.
 *
 * 문서 근거를 코드와 함께 보냅니다. 근거 없이 코드만 주면 INTENT·서술형이 의도를 지어내는데,
 * 식별자는 실제 코드에서 가져오므로 뒷단계의 식별자 검증에도 걸리지 않습니다.
 */
@Component
class GeminiQuestionWriter(
    builder: ChatClient.Builder,
    @Value("\${ai.model.question}") model: String,
) : QuestionWriter {
    private val call = StructuredChatCall(builder, "Question generation", model, SYSTEM_PROMPT, LearningSetDraft::class.java)

    override fun write(
        concept: Concept,
        anchorBundle: String,
        depth: Depth,
    ): LearningSetDraft {
        val userText =
            """
            |개념: ${concept.name}
            |문서 근거: ${concept.rationale}
            |이번에 쓸 레벨: $depth
            |
            |$anchorBundle
            """.trimMargin()

        return call.call(userText)
            ?: throw BaseException(ErrorCode.QUESTION_GENERATION_FAILED, "문제 생성 응답을 해석하지 못했습니다")
    }

    companion object {
        private val SYSTEM_PROMPT = ClassPathResource("prompts/question-generation.st")
    }
}
