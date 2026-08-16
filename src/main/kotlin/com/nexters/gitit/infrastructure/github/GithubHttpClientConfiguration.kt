package com.nexters.gitit.infrastructure.github

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class GithubHttpClientConfiguration {
    /**
     * zipball은 codeload로 302 리다이렉트하므로 추종하지 않으면 본문이 비어 옵니다.
     */
    @Bean
    fun githubClient(): HttpClient =
        HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(CONNECT_TIMEOUT)
            .build()

    companion object {
        private val CONNECT_TIMEOUT = Duration.ofSeconds(10)
    }
}
