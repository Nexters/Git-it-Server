package com.nexters.gitit.infrastructure.ai

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.core.Ordered
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * 콜 하나가 얼마나 걸렸고 토큰을 얼마나 썼는지 남깁니다.
 *
 * 프롬프트를 고쳤을 때 입력 토큰이 늘었는지 줄었는지 비교할 근거가 없으면
 * 문서 번들 예산을 조정할 수 없고, 레포당 비용도 산정할 수 없습니다.
 *
 * 계측은 흐름을 바꾸지 않습니다. 응답은 받은 그대로 돌려주고 예외도 그대로 다시 던집니다.
 */
class ChatCallLoggingAdvisor : CallAdvisor {
    override fun adviseCall(
        chatClientRequest: ChatClientRequest,
        callAdvisorChain: CallAdvisorChain,
    ): ChatClientResponse {
        // 콜 하나가 분 단위라, 끝날 때만 찍으면 도는 중인지 멈춘 건지 로그로 구분되지 않는다.
        logger.info { "Chat call started. ${describe(chatClientRequest)}" }

        val startedAt = System.nanoTime()
        val result = runCatching { callAdvisorChain.nextCall(chatClientRequest) }
        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

        result
            .onSuccess { logger.info { "Chat call completed. elapsed=${elapsedMillis}ms ${describe(it)}" } }
            .onFailure { logger.warn(it) { "Chat call failed. elapsed=${elapsedMillis}ms" } }

        return result.getOrThrow()
    }

    private fun describe(request: ChatClientRequest): String =
        "model=${request.prompt().options?.model ?: "unknown"} promptChars=${request.prompt().contents.length}"

    /**
     * 모델명과 토큰 수를 로그 한 줄에 붙일 형태로 만듭니다. 없는 항목은 그냥 빠집니다.
     *
     * 응답 본체도 토큰 집계도 없을 수 있어서, 계측 때문에 파이프라인이 죽지 않도록 전부 선택적으로 다룹니다.
     */
    private fun describe(response: ChatClientResponse): String {
        val metadata = response.chatResponse()?.metadata ?: return "model=unknown"
        val usage = metadata.usage

        return "model=${metadata.model} promptTokens=${usage.promptTokens} " +
            "completionTokens=${usage.completionTokens} totalTokens=${usage.totalTokens}"
    }

    override fun getName(): String = javaClass.simpleName

    // 체인의 가장 바깥에 둔다. 다른 어드바이저가 요청을 손보는 시간까지 포함해야 실제 소요 시간이다.
    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
