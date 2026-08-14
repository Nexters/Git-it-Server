package com.nexters.gitit.infrastructure.github

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 목으로 막으면 우리가 짠 try/catch와 RestClient 동작만 확인하게 됩니다. 알고 싶은 것은 "이 URL 규칙과 응답
 * 파싱이 진짜 GitHub에서 통하는가"라서 실제로 부릅니다.
 */
@Tag("network")
class GithubApiRepositoryResolverTest {
    private val resolver = GithubApiRepositoryResolver(GithubClientConfiguration().githubRestClient())

    @Test
    fun `공개된 저장소면 GitHub id를 반환한다`() {
        resolver.resolve("https://github.com/Nexters/Git-it-Server") shouldBe "1310710749"
    }

    @Test
    fun `GitHub에 없는 저장소면 null을 반환한다`() {
        resolver.resolve("https://github.com/nexters/no-such-repository-for-git-it") shouldBe null
    }
}
