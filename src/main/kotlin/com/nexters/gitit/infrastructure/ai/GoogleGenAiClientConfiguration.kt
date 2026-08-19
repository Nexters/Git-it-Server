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
     * 자동 설정은 서비스 계정을 파일 경로(`credentials-uri`)로만 받습니다. 배포가 GCP 밖(도커 호스트)이라
     * 키 파일을 마운트하는 배관 대신 한 줄짜리 Base64를 환경 변수로 실어 보냅니다.
     *
     * 자동 설정의 클라이언트 빈이 `@ConditionalOnMissingBean`이라 이 빈이 대신 쓰이고, 나머지 배선
     * (ChatModel·ChatClient.Builder)은 그대로 자동 설정이 합니다. 자격 증명이 없는 곳에서는 이 빈이
     * 만들어지지 않아야 컨텍스트가 뜨므로 프로퍼티가 있을 때만 답니다.
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

    private fun credentials(base64: String): GoogleCredentials {
        // 값이 비어도 프로퍼티는 "있는" 것이라 빈이 만들어지고, 그대로 두면 JSON 파싱 오류로 죽는다.
        // 기동 실패는 그대로 두되(설정 실수는 배포 순간에 드러나야 한다) 무엇을 안 채웠는지는 알려준다.
        require(base64.isNotBlank()) { "GCP_CREDENTIALS_BASE64가 비어 있습니다" }

        // MIME 디코더는 base64 알파벳 밖 문자를 무시한다. 인코딩 도구가 76자마다 개행을 넣기도 해서,
        // 엄격한 디코더로 받으면 값을 만든 사람의 손버릇에 따라 기동이 실패한다.
        return Base64
            .getMimeDecoder()
            .decode(base64)
            .inputStream()
            .use { GoogleCredentials.fromStream(it) }
            // 서비스 계정 JSON은 스코프가 비어 있는 채로 만들어져, 그대로 쓰면 토큰 발급에서 거부당한다.
            .createScoped(CLOUD_PLATFORM_SCOPE)
    }

    companion object {
        private const val CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform"
    }
}
