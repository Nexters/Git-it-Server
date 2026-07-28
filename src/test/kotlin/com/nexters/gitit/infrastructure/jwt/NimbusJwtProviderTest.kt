package com.nexters.gitit.infrastructure.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.kotest.inspectors.forAll
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

class NimbusJwtProviderTest {
    // 고정 시계라 iat/exp를 오차 허용 없이 그대로 단언할 수 있다.
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val provider =
        NimbusJwtProvider(
            secret = SECRET,
            accessTokenExpirationDays = ACCESS_TOKEN_EXPIRATION_DAYS,
            refreshTokenExpirationDays = REFRESH_TOKEN_EXPIRATION_DAYS,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

    @Test
    fun `accessToken에서 type, memberId, issuedAt, expireAt을 추출한다`() {
        val claims = claimsOf(provider.generateToken(MEMBER_ID).accessToken)

        claims.getStringClaim(TOKEN_TYPE) shouldBe "accessToken"
        claims.subject shouldBe MEMBER_ID
        claims.issueTime shouldBe Date.from(now)
        claims.expirationTime shouldBe Date.from(now + Duration.ofDays(ACCESS_TOKEN_EXPIRATION_DAYS))
    }

    @Test
    fun `refreshToken에서 type, memberId, issuedAt, expireAt을 추출한다`() {
        val claims = claimsOf(provider.generateToken(MEMBER_ID).refreshToken)

        claims.getStringClaim(TOKEN_TYPE) shouldBe "refreshToken"
        claims.subject shouldBe MEMBER_ID
        claims.issueTime shouldBe Date.from(now)
        claims.expirationTime shouldBe Date.from(now + Duration.ofDays(REFRESH_TOKEN_EXPIRATION_DAYS))
    }

    @Test
    fun `accessToken과 refreshToken은 서로 다른 토큰이다`() {
        val token = provider.generateToken(MEMBER_ID)

        token.accessToken shouldNotBe token.refreshToken
    }

    @Test
    fun `발급한 토큰은 HS256으로 서명되어 시크릿으로 검증된다`() {
        val token = provider.generateToken(MEMBER_ID)

        listOf(token.accessToken, token.refreshToken).forAll {
            val signedJwt = SignedJWT.parse(it)

            signedJwt.header.algorithm shouldBe JWSAlgorithm.HS256
            signedJwt.verify(MACVerifier(SECRET.toByteArray())).shouldBeTrue()
        }
    }

    @Test
    fun `다른 시크릿으로는 서명 검증에 실패한다`() {
        val token = provider.generateToken(MEMBER_ID)

        SignedJWT.parse(token.accessToken).verify(MACVerifier(OTHER_SECRET.toByteArray())).shouldBeFalse()
    }

    private fun claimsOf(token: String): JWTClaimsSet = SignedJWT.parse(token).jwtClaimsSet

    companion object {
        // HS256은 시크릿이 최소 32바이트여야 MACSigner 생성에 성공한다.
        private const val SECRET = "verylongsecretkeyfordevelopmentenvironmentonly"
        private const val OTHER_SECRET = "totallydifferentsecretkeythatisalsolongenough"
        private const val ACCESS_TOKEN_EXPIRATION_DAYS = 30L
        private const val REFRESH_TOKEN_EXPIRATION_DAYS = 90L
        private const val MEMBER_ID = "member-1"
        private const val TOKEN_TYPE = "type"
    }
}
