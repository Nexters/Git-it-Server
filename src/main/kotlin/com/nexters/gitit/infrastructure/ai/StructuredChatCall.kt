package com.nexters.gitit.infrastructure.ai

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.core.io.Resource

private val logger = KotlinLogging.logger {}

/**
 * 구조화 출력을 요구하는 콜 한 번. 파싱이 깨지면 오류를 붙여 딱 한 번 다시 묻고, 그래도 깨지면
 * null입니다 — 실패를 어떤 [com.nexters.gitit.domain.exception.ErrorCode]로 볼지는 단계마다 달라
 * 호출자가 정합니다.
 *
 * 응답을 객체가 아니라 원문 문자열로 받아 직접 변환합니다. 파싱이 실패했을 때 무엇이 왔는지가
 * 로그에 남아야 프롬프트를 고칠 수 있습니다.
 *
 * [model]은 콜마다 받습니다. 단계가 요구하는 능력이 서로 달라, 결과를 코드와 대조해 버릴 수 있는
 * 앵커 고르기는 싼 모델로 충분하지만 문제 생성은 그렇지 않습니다.
 */
class StructuredChatCall<T : Any>(
    builder: ChatClient.Builder,
    private val stage: String,
    private val model: String,
    private val systemPrompt: Resource,
    responseType: Class<T>,
) {
    private val chatClient = builder.build()
    private val converter = BeanOutputConverter(responseType)

    fun call(userText: String): T? {
        val first = call(userText, correction = null)
        parse(first)?.let { return it }

        // 스키마를 강제해도 형식이 깨질 때가 있다. 무엇이 잘못됐는지 알려주고 딱 한 번 더 묻는다.
        logger.warn { "$stage returned unparseable output, retrying once. raw=$first" }
        val second = call(userText, correction = first)

        return parse(second) ?: run {
            logger.warn { "$stage retry also failed. raw=$second" }
            null
        }
    }

    private fun call(
        userText: String,
        correction: String?,
    ): String {
        // 스키마를 모델에 직접 강제한다. 프롬프트로 형식을 부탁하는 것보다 파싱 실패가 훨씬 줄어든다.
        // outputSchema는 응답 MIME 타입까지 함께 잡아 주므로 JSON을 따로 요구하지 않는다.
        val optionsBuilder =
            GoogleGenAiChatOptions
                .builder()
                .model(model)
                .outputSchema(converter.jsonSchema)

        val text =
            if (correction == null) {
                userText
            } else {
                """
                |직전 응답이 요구한 JSON 형식과 맞지 않았습니다. 형식을 지켜 다시 답하세요.
                |
                |직전 응답:
                |$correction
                |
                |$userText
                """.trimMargin()
            }

        return backingOff {
            chatClient
                .prompt()
                .options(optionsBuilder)
                .system(systemPrompt)
                .user(text)
                .call()
                .content()
                .orEmpty()
        }
    }

    /**
     * 쿼터에 걸린 콜만 기다렸다 다시 보냅니다.
     *
     * Gemini 2.5는 지역 공유 쿼터를 쓰므로 아무리 적게 쏴도 429가 납니다. 정상 동작의 일부라
     * 여기서 흡수하지 않으면 개념 하나가 통째로 날아갑니다.
     *
     * 다른 실패는 즉시 올려 보냅니다 — 재시도로 안 풀리는 것을 붙들면 실패를 확인하는 데만 몇 분이 걸립니다.
     */
    private fun backingOff(block: () -> String): String {
        repeat(RATE_LIMIT_ATTEMPTS - 1) { attempt ->
            runCatching(block)
                .onSuccess { return it }
                .onFailure { if (!it.isRateLimited()) throw it }

            val waitMillis = RATE_LIMIT_BACKOFF_MILLIS shl attempt
            logger.warn { "$stage hit the rate limit, retrying in ${waitMillis}ms" }
            Thread.sleep(waitMillis)
        }

        return block()
    }

    // SDK가 쿼터 초과를 별도 예외 타입으로 주지 않아 메시지로 가른다. 원인 사슬을 훑는 이유는 감싸여 올라오기 때문이다.
    private fun Throwable.isRateLimited(): Boolean =
        generateSequence(this) { it.cause }.any { cause ->
            cause.message?.let { "429" in it || "RESOURCE_EXHAUSTED" in it } == true
        }

    private fun parse(raw: String): T? = runCatching { converter.convert(raw) }.getOrNull()

    companion object {
        private const val RATE_LIMIT_ATTEMPTS = 4

        // 쿼터가 분 단위로 회복되므로 첫 대기부터 그 주기에 가까워야 한다. 짧게 잡으면 같은 창에서 또 맞고 재시도만 태운다.
        private const val RATE_LIMIT_BACKOFF_MILLIS = 30_000L
    }
}
