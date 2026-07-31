package com.nexters.gitit.infrastructure.jwt

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.kotest.assertions.throwables.shouldThrow
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
    private val provider = providerAt(now)

    // 시크릿만 다른 서버가 발급한 토큰을 흉내내기 위한 프로바이더
    private val otherSecretProvider = providerAt(now, secret = OTHER_SECRET)

    // accessToken 만료 이후로 시계를 옮긴 프로바이더. 만료 검사가 주입된 clock을 쓰는지 확인한다.
    private val afterExpirationProvider = providerAt(now + Duration.ofDays(ACCESS_TOKEN_EXPIRATION_DAYS + 1))

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

    @Test
    fun `accessToken을 검증하면 memberId를 반환한다`() {
        val accessToken = provider.generateToken(MEMBER_ID).accessToken

        provider.verifyAccessToken(accessToken) shouldBe MEMBER_ID
    }

    @Test
    fun `refreshToken은 accessToken으로 검증되지 않는다`() {
        val refreshToken = provider.generateToken(MEMBER_ID).refreshToken

        shouldThrowUnauthorized { provider.verifyAccessToken(refreshToken) }
    }

    @Test
    fun `만료된 accessToken은 검증에 실패한다`() {
        val accessToken = provider.generateToken(MEMBER_ID).accessToken

        shouldThrowUnauthorized { afterExpirationProvider.verifyAccessToken(accessToken) }
    }

    @Test
    fun `다른 시크릿으로 서명된 accessToken은 검증에 실패한다`() {
        val accessToken = otherSecretProvider.generateToken(MEMBER_ID).accessToken

        shouldThrowUnauthorized { provider.verifyAccessToken(accessToken) }
    }

    @Test
    fun `JWT 형식이 아닌 문자열은 검증에 실패한다`() {
        shouldThrowUnauthorized { provider.verifyAccessToken("not-a-jwt") }
    }

    private fun claimsOf(token: String): JWTClaimsSet = SignedJWT.parse(token).jwtClaimsSet

    private fun shouldThrowUnauthorized(block: () -> Unit) {
        shouldThrow<BaseException>(block).errorCode shouldBe ErrorCode.UNAUTHORIZED
    }

    private fun providerAt(
        instant: Instant,
        secret: String = SECRET,
    ) = NimbusJwtProvider(
        secret = secret,
        accessTokenExpirationDays = ACCESS_TOKEN_EXPIRATION_DAYS,
        refreshTokenExpirationDays = REFRESH_TOKEN_EXPIRATION_DAYS,
        clock = Clock.fixed(instant, ZoneOffset.UTC),
    )

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
