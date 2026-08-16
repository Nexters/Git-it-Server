package com.nexters.gitit.infrastructure.github

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveAtMostSize
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 목으로 막으면 우리가 짠 try/catch와 RestClient 동작만 확인하게 됩니다. 알고 싶은 것은 "이 URL 규칙과 응답
 * 파싱이 진짜 GitHub에서 통하는가"라서 실제로 부릅니다 — 특히 `stargazers_count`·`avatar_url`은 이름 전략이
 * 기본값이라 손으로 짚어준 매핑이고, 가짜 응답으로는 그게 맞는지 알 수 없습니다.
 *
 * 원격 값은 언제든 변하므로 흔들리는 값은 정확히 맞히지 않고 모양만 봅니다.
 */
@Tag("network")
class GithubApiRepositoryResolverTest {
    private val resolver = GithubApiRepositoryResolver(GithubClientConfiguration().githubRestClient())

    @Test
    fun `공개된 저장소면 식별자와 함께 보여줄 정보를 채워 반환한다`() {
        val repository = resolver.resolve("https://github.com/Nexters/Git-it-Server").shouldNotBeNull()

        repository.id shouldBe "1310710749"
        repository.name shouldBe "Git-it-Server"
        repository.ownerImageUrl shouldStartWith "https://avatars.githubusercontent.com/"
        repository.starCount shouldBeGreaterThanOrEqual 0
    }

    @Test
    fun `topics를 기술 스택으로 쓰되 세 개까지만 쓴다`() {
        val repository = resolver.resolve("https://github.com/spring-projects/spring-boot").shouldNotBeNull()

        repository.techStacks shouldContain "spring"
        repository.techStacks shouldHaveAtMostSize 3
    }

    @Test
    fun `GitHub에 없는 저장소면 null을 반환한다`() {
        resolver.resolve("https://github.com/nexters/no-such-repository-for-git-it") shouldBe null
    }
}
