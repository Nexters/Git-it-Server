package com.nexters.gitit.infrastructure.github

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient

/**
 * 여기서 새면 사용자가 준 적 없는 저장소가 등록되므로, 실제 호출이 필요한 `GithubApiRepositoryResolverTest`와 달리
 * 기본 test에 두어 늘 돌게 합니다.
 *
 * 클라이언트를 닿지 않는 주소로 둔 것은 "걸러진다"와 "호출조차 안 한다"를 함께 확인하려는 것입니다.
 * 요청이 한 번이라도 나가면 null이 아니라 예외로 실패합니다.
 */
class GithubRepoUrlValidationTest {
    private val resolver = GithubApiRepositoryResolver(RestClient.create("http://localhost:1"))

    @Test
    fun `호스트가 github_com이 아니면 등록을 거절한다`() {
        resolver.resolve("https://notgithub.com/Nexters/Git-it-Server") shouldBe null
        resolver.resolve("https://github.com.nexters.com/Nexters/Git-it-Server") shouldBe null
        resolver.resolve("https://gitit.nexters.com/o/n?ref=github.com/Nexters/Git-it-Server") shouldBe null
    }

    @Test
    fun `저장소 하나를 가리키지 않으면 등록을 거절한다`() {
        resolver.resolve("https://github.com/Nexters") shouldBe null
        resolver.resolve("https://github.com/Nexters/Git-it-Server/tree/main") shouldBe null
    }
}
