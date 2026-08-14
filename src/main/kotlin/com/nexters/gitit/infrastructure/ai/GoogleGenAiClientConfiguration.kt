package com.nexters.gitit.infrastructure.ai

import com.google.auth.oauth2.GoogleCredentials
import com.google.genai.Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Base64

@Configuration
class GoogleGenAiClientConfiguration {
    /**
     * Vertex AI에 붙는 클라이언트. 서비스 계정 자격 증명을 Base64 한 줄로 받습니다.
     *
     * 자동 설정도 서비스 계정을 받지만 `credentials-uri`, 즉 **파일 경로**로만 받습니다. 배포가 GCP 밖
     * (도커 호스트)이라 그 길로 가면 키 파일을 호스트에 내려놓고 컨테이너에 마운트하는 배관이 CD와
     * compose에 따라붙습니다. 한 줄짜리 Base64는 이미 있는 `.env` 전달 경로에 그대로 실립니다.
     *
     * API 키(express mode)를 쓰지 않는 이유는 접근 제어입니다. 키는 문자열 하나가 곧 권한이라 회수도
     * 감사도 안 되고, 쓸 수 있는 모델도 정식 Vertex보다 좁습니다.
     *
     * 자동 설정의 클라이언트 빈이 `@ConditionalOnMissingBean`이라 이것이 대신 쓰이고, 나머지 배선
     * (ChatModel·ChatClient.Builder)은 그대로 자동 설정이 합니다. 자격 증명이 없는 곳에서는 이 빈이
     * 아예 만들어지지 않아야 컨텍스트가 뜨므로 프로퍼티가 있을 때만 답니다.
     */
    @Bean
    @ConditionalOnProperty("gcp.credentials-base64")
    fun googleGenAiClient(
        @Value("\${gcp.project-id}") projectId: String,
        @Value("\${gcp.location}") location: String,
        @Value("\${gcp.credentials-base64}") credentialsBase64: String,
    ): Client =
        Client
            .builder()
            .vertexAI(true)
            .project(projectId)
            .location(location)
            .credentials(credentials(credentialsBase64))
            .build()

    // MIME 디코더는 base64 알파벳 밖 문자를 무시한다. 인코딩 도구가 76자마다 개행을 넣기도 해서,
    // 엄격한 디코더로 받으면 값을 만든 사람의 손버릇에 따라 기동이 실패한다.
    private fun credentials(base64: String): GoogleCredentials =
        Base64
            .getMimeDecoder()
            .decode(base64)
            .inputStream()
            .use { GoogleCredentials.fromStream(it) }
            // 서비스 계정 JSON은 스코프가 비어 있는 채로 만들어져, 그대로 쓰면 토큰 발급에서 거부당한다.
            .createScoped(CLOUD_PLATFORM_SCOPE)

    companion object {
        private const val CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform"
    }
}
