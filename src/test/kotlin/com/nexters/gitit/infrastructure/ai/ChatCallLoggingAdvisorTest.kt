package com.nexters.gitit.infrastructure.ai

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt

class ChatCallLoggingAdvisorTest {
    private val advisor = ChatCallLoggingAdvisor()
    private val request = ChatClientRequest.builder().prompt(Prompt("문서 번들")).build()

    @Test
    fun `토큰이 담긴 응답을 그대로 돌려준다`() {
        val response = chatClientResponse(withMetadata = true)

        advisor.adviseCall(request, chainReturning(response)) shouldBe response
    }

    @Test
    fun `응답 본체가 없어도 통과시킨다`() {
        val response = chatClientResponse(withMetadata = false)

        advisor.adviseCall(request, chainReturning(response)) shouldBe response
    }

    @Test
    fun `콜이 실패하면 예외를 그대로 다시 던진다`() {
        val chain = chain { throw IllegalStateException("호출 실패") }

        shouldThrow<IllegalStateException> { advisor.adviseCall(request, chain) }.message shouldBe "호출 실패"
    }

    private fun chainReturning(response: ChatClientResponse) = chain { response }

    private fun chain(next: () -> ChatClientResponse) =
        object : CallAdvisorChain {
            override fun nextCall(chatClientRequest: ChatClientRequest) = next()

            override fun getCallAdvisors(): List<CallAdvisor> = emptyList()

            override fun copy(callAdvisor: CallAdvisor): CallAdvisorChain = this
        }

    private fun chatClientResponse(withMetadata: Boolean): ChatClientResponse {
        if (!withMetadata) return ChatClientResponse.builder().build()

        val metadata =
            ChatResponseMetadata
                .builder()
                .model("gemini-2.5-flash")
                .usage(DefaultUsage(8_120, 642))
                .build()
        val chatResponse =
            ChatResponse
                .builder()
                .generations(listOf(Generation(AssistantMessage("응답"))))
                .metadata(metadata)
                .build()

        return ChatClientResponse.builder().chatResponse(chatResponse).build()
    }
}
