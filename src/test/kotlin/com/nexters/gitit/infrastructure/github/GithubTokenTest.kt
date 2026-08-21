package com.nexters.gitit.infrastructure.github

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

internal fun githubToken(): String = System.getenv("GH_TOKEN").orEmpty()

/**
 * 헤더를 실었다는 것과 GitHub이 그 헤더를 받아들였다는 것은 다릅니다. 공개 레포는 무인증으로도 200이 와서
 * 응답 코드로는 구분되지 않으므로, 쿼터가 무인증 한도를 넘는지로 실제 인증 여부를 봅니다.
 */
@Tag("network")
class GithubTokenTest {
    @Test
    fun `토큰을 붙이면 무인증 한도를 넘는 쿼터로 응답한다`() {
        assumeTrue(githubToken().isNotBlank(), "GH_TOKEN이 없으면 확인할 수 없다")

        val rateLimit =
            GithubClientConfiguration(githubToken())
                .githubRestClient()
                .get()
                .uri("/rate_limit")
                .retrieve()
                .body(JsonNode::class.java)
                .shouldNotBeNull()

        rateLimit["resources"]["core"]["limit"].asInt() shouldBeGreaterThan UNAUTHENTICATED_LIMIT
    }

    companion object {
        private const val UNAUTHENTICATED_LIMIT = 60
    }
}
