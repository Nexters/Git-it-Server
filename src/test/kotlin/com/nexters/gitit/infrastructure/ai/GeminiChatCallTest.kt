package com.nexters.gitit.infrastructure.ai

import com.google.auth.oauth2.GoogleCredentials
import com.google.genai.Client
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.google.genai.GoogleGenAiChatModel
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.core.io.ByteArrayResource
import java.util.Base64

/**
 * Gemini에 실제로 말이 통하는지 봅니다.
 *
 * 목으로는 확인할 수 없는 것만 봅니다 — 서비스 계정으로 인증이 잡히는지, 고른 리전에 그 모델이 있는지,
 * 그리고 [StructuredChatCall]이 거는 `outputSchema`를 Gemini가 실제로 받아주는지.
 * 셋 다 코드가 아니라 프로젝트 설정에 달린 문제라, 여기서 안 보면 파이프라인을 통째로 돌려야 알 수 있습니다.
 *
 * 스프링 컨텍스트를 띄우지 않는 이유는 이 확인에 MongoDB가 필요 없기 때문입니다.
 */
@Tag("network")
class GeminiChatCallTest {
    private val credentialsBase64: String? = System.getenv("GCP_CREDENTIALS_BASE64")

    @Test
    fun `평문 왕복이 된다`() {
        val chatClient = ChatClient.builder(chatModel()).build()

        val answer =
            chatClient
                .prompt()
                .options(options(CHEAP_MODEL))
                .user("Reply with exactly: hello, git-it")
                .call()
                .content()

        answer shouldNotBe null
        answer!!.lowercase() shouldContain "hello, git-it"
    }

    /**
     * `ai.model.*`에 걸 후보를 이 프로젝트·리전에서 부를 수 있는지 봅니다.
     *
     * SDK가 아는 모델을 전부 돌지 않습니다. 안 쓸 모델이 빨간불이어도 할 일이 없고, 콜만 나갑니다.
     * 여기 있는 것은 지금 세 단계에 건 셋뿐입니다.
     *
     * 평문이 아니라 구조화 출력으로 부르는 이유는 파이프라인이 그것만 쓰기 때문입니다. 말은 하는데
     * 스키마를 거부하는 모델은 우리에겐 못 쓰는 모델이라, 평문으로 통과시키면 목록이 거짓말이 됩니다.
     *
     * 실패한 항목의 이름이 곧 "여기서는 못 부르는 모델"입니다. 리전마다 제공 모델이 달라
     * 프리뷰가 걸릴 가능성이 가장 큽니다.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "gemini-2.5-flash", // ai.model.concept
            "gemini-2.5-flash-lite", // ai.model.anchor
            "gemini-2.5-pro", // ai.model.question
        ],
    )
    fun `문제 생성에 걸 모델을 이 리전에서 부를 수 있다`(model: String) {
        val call =
            StructuredChatCall(
                ChatClient.builder(chatModel()),
                "Smoke test",
                model,
                ByteArrayResource(SYSTEM_PROMPT.toByteArray()),
                Greeting::class.java,
            )

        val greeting = call.call("git-it에게 건넬 인사말 하나와, 그 인사말의 글자 수를 채우세요.")

        greeting shouldNotBe null
        greeting!!.message shouldNotBe ""
    }

    private fun chatModel(): GoogleGenAiChatModel {
        assumeTrue(credentialsBase64 != null, "GCP_CREDENTIALS_BASE64가 없으면 부를 곳이 없다")

        // 운영과 같은 방식으로 붙는다 — GoogleGenAiClientConfiguration이 만드는 것과 같은 클라이언트다.
        val credentials =
            Base64
                .getMimeDecoder()
                .decode(credentialsBase64)
                .inputStream()
                .use { GoogleCredentials.fromStream(it) }
                .createScoped("https://www.googleapis.com/auth/cloud-platform")

        val client =
            Client
                .builder()
                .vertexAI(true)
                .project(PROJECT_ID)
                .location(LOCATION)
                .credentials(credentials)
                .build()

        return GoogleGenAiChatModel.builder().genAiClient(client).build()
    }

    private fun options(model: String) = GoogleGenAiChatOptions.builder().model(model)

    /** 구조화 출력 확인용. 중첩 없이 얕게 두어, 실패하면 스키마 모양이 아니라 모델이 문제라는 뜻이 되게 합니다. */
    data class Greeting(
        val message: String,
        val length: Int,
    )

    companion object {
        // application.yaml에 박아 둔 값과 같아야 한다. 다르면 여기서 초록인 모델이 운영에서 404가 난다.
        private const val PROJECT_ID = "git-it-503911"
        private const val LOCATION = "us-central1"
        private const val CHEAP_MODEL = "gemini-2.5-flash-lite"
        private const val SYSTEM_PROMPT = "당신은 인사말을 만드는 도우미입니다."
    }
}
