package com.nexters.gitit.application

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.auth.OauthAuthenticator
import com.nexters.gitit.domain.auth.OauthCredential
import com.nexters.gitit.domain.auth.SocialAccount
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.member.SocialIdentity
import com.nexters.gitit.domain.member.SocialType
import com.nimbusds.jwt.SignedJWT
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class LoginTest(
    @Autowired private val login: Login,
    @Autowired private val memberRepository: MemberRepository,
) {
    // provider 검증은 Google/Apple 공개키 조회가 필요해 테스트에서 진짜로 돌릴 수 없다.
    // 나머지 협력자(MongoDB, JwtProvider)는 실제 구현을 쓴다.
    @MockitoBean
    private lateinit var oauthAuthenticator: OauthAuthenticator

    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        memberRepository.deleteAll()
    }

    @Test
    fun `첫 로그인이면 회원을 저장하고 그 회원의 토큰을 발급한다`() {
        given(oauthAuthenticator.authenticate(CREDENTIAL)).willReturn(socialAccount(email = "gitit@nexters.com"))

        val result = login(Login.Command(CREDENTIAL))

        val saved = memberRepository.findById(result.memberId).orElse(null).shouldNotBeNull()
        saved.socialIdentity shouldBe SOCIAL_IDENTITY
        saved.email shouldBe "gitit@nexters.com"
        memberRepository.count() shouldBe 1
        subjectOf(result.jwtToken.accessToken) shouldBe result.memberId
    }

    @Test
    fun `이미 가입한 회원이면 새로 저장하지 않고 기존 회원의 토큰을 발급한다`() {
        given(oauthAuthenticator.authenticate(CREDENTIAL)).willReturn(socialAccount(email = "gitit@nexters.com"))
        val firstLogin = login(Login.Command(CREDENTIAL))

        val secondLogin = login(Login.Command(CREDENTIAL))

        secondLogin.memberId shouldBe firstLogin.memberId
        memberRepository.count() shouldBe 1
        subjectOf(secondLogin.jwtToken.accessToken) shouldBe firstLogin.memberId
    }

    private fun socialAccount(email: String?) = SocialAccount(socialIdentity = SOCIAL_IDENTITY, email = email)

    private fun subjectOf(accessToken: String): String = SignedJWT.parse(accessToken).jwtClaimsSet.subject

    companion object {
        private val SOCIAL_IDENTITY = SocialIdentity("google-social-id", SocialType.GOOGLE)
        private val CREDENTIAL = OauthCredential.Google("google-id-token")
    }
}
