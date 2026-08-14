package com.nexters.gitit.infrastructure.github

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

/**
 * GitHub API 호출용 클라이언트를 빈으로 둡니다.
 *
 * 호출부에서 직접 만들면 인증 헤더나 타임아웃 같은 공통 설정을 나중에 붙일 때 손댈 자리가 여러 곳으로
 * 흩어지고, 테스트에서 가짜 클라이언트로 바꿔 끼울 수도 없습니다.
 */
@Configuration
class GithubClientConfiguration {
    @Bean
    fun githubRestClient(): RestClient = RestClient.create(GITHUB_API_BASE_URL)

    companion object {
        private const val GITHUB_API_BASE_URL = "https://api.github.com"
    }
}
