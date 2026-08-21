package com.nexters.gitit.infrastructure.github

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.io.IOException
import java.time.Duration

/**
 * GitHub API 호출용 클라이언트를 빈으로 둡니다.
 *
 * 호출부에서 직접 만들면 인증 헤더나 타임아웃 같은 공통 설정을 나중에 붙일 때 손댈 자리가 여러 곳으로
 * 흩어지고, 테스트에서 가짜 클라이언트로 바꿔 끼울 수도 없습니다.
 */
@Configuration
class GithubClientConfiguration(
    @Value("\${github.token:}") private val token: String,
) {
    @Bean
    fun githubRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl(GITHUB_API_BASE_URL)
            .requestFactory(timeoutBoundRequestFactory())
            .requestInterceptor { request, body, execution -> RETRY.execute { execution.execute(request, body) } }
            // 빈 Bearer는 무인증이 아니라 잘못된 자격증명으로 읽혀 401이 된다.
            .defaultHeaders { if (token.isNotBlank()) it.setBearerAuth(token) }
            .build()

    /**
     * 저장소 하나를 읽는 단건 조회라 정상이면 수백 ms 안에 끝납니다.
     *
     * 타임아웃은 재시도 횟수와 곱해져 그대로 사용자 대기 시간이 됩니다 — `(연결 + 읽기) × (재시도 + 1) + 지연`.
     * 값을 올릴 때는 이 곱을 같이 보고 정해야 합니다.
     */
    private fun timeoutBoundRequestFactory() =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(CONNECT_TIMEOUT)
            setReadTimeout(READ_TIMEOUT)
        }

    companion object {
        private const val GITHUB_API_BASE_URL = "https://api.github.com"

        private val CONNECT_TIMEOUT = Duration.ofSeconds(1)
        private val READ_TIMEOUT = Duration.ofSeconds(1)
        private val RETRY_DELAY = Duration.ofMillis(200)
        private const val MAX_RETRIES = 1L

        // 조회뿐이라 몇 번을 보내도 부작용이 없다. 연결이 한 번 끊겼다고 등록이 실패하지 않을 만큼만 짧게 둔다.
        // 응답을 받아낸 뒤의 5xx는 여기서 재시도하지 않는다 — 그건 GitHub이 실제로 답을 준 상태라 성격이 다르다.
        private val RETRY =
            RetryTemplate(
                RetryPolicy
                    .builder()
                    .maxRetries(MAX_RETRIES)
                    .delay(RETRY_DELAY)
                    .includes(IOException::class.java)
                    .build(),
            )
    }
}
