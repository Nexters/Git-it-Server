package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.infrastructure.jwt.NimbusJwtProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class VerifyMemberTest {
    // 협력자가 JwtProvider 하나뿐이라 컨텍스트를 띄우지 않는다.
    // 위임만 하는 클래스라 mock을 물리면 자기 자신을 검증하는 꼴이 되므로 실제 구현을 쓴다.
    private val jwtProvider =
        NimbusJwtProvider(
            secret = SECRET,
            accessTokenExpirationDays = ACCESS_TOKEN_EXPIRATION_DAYS,
            refreshTokenExpirationDays = REFRESH_TOKEN_EXPIRATION_DAYS,
            clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
        )
    private val verifyMember = VerifyMember(jwtProvider)

    @Test
    fun `발급받은 accessToken으로 인증하면 그 회원의 memberId를 반환한다`() {
        val accessToken = jwtProvider.generateToken(MEMBER_ID).accessToken

        verifyMember(accessToken) shouldBe MEMBER_ID
    }

    @Test
    fun `refreshToken으로는 인증되지 않는다`() {
        val refreshToken = jwtProvider.generateToken(MEMBER_ID).refreshToken

        shouldThrow<BaseException> { verifyMember(refreshToken) }.errorCode shouldBe ErrorCode.UNAUTHORIZED
    }

    @Test
    fun `토큰 검증에 실패하면 예외를 그대로 전파한다`() {
        shouldThrow<BaseException> { verifyMember("not-a-jwt") }.errorCode shouldBe ErrorCode.UNAUTHORIZED
    }

    companion object {
        // HS256은 시크릿이 최소 32바이트여야 MACSigner 생성에 성공한다.
        private const val SECRET = "verylongsecretkeyfordevelopmentenvironmentonly"
        private const val ACCESS_TOKEN_EXPIRATION_DAYS = 30L
        private const val REFRESH_TOKEN_EXPIRATION_DAYS = 90L
        private const val MEMBER_ID = "member-1"
    }
}
