package com.nexters.gitit.infrastructure.jwt

import com.nexters.gitit.domain.auth.JwtProvider
import com.nexters.gitit.domain.auth.JwtToken
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.Date

private val logger = KotlinLogging.logger {}

@Component
class NimbusJwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access_token.expiration_days}") private val accessTokenExpirationDays: Long,
    @Value("\${jwt.refresh_token.expiration_days}") private val refreshTokenExpirationDays: Long,
    private val clock: Clock,
) : JwtProvider {
    companion object {
        private const val TOKEN_TYPE = "type"
    }

    /**
     * accessToken과 refreshToken은 만료 기간만 다른 같은 형식입니다.
     * 발급 시 type 클레임에 [claimValue]를 넣고 검증에서 대조해야 refreshToken으로 API를 호출하는 것을 막을 수 있습니다.
     */
    private enum class TokenType(
        val claimValue: String,
        val label: String,
    ) {
        ACCESS("accessToken", "액세스 토큰"),
        REFRESH("refreshToken", "리프레시 토큰"),
    }

    /** 만료만 로그 레벨을 달리 두기 위해 나머지 검증 실패와 구분합니다. */
    private class ExpiredTokenException : RuntimeException("expired")

    private val signer = MACSigner(secret.toByteArray())
    private val verifier = MACVerifier(secret.toByteArray())

    override fun generateToken(memberId: String): JwtToken =
        JwtToken(
            accessToken = generateAccessToken(memberId),
            refreshToken = generateRefreshToken(memberId),
        )

    override fun verifyAccessToken(accessToken: String): String = verify(accessToken, TokenType.ACCESS)

    private fun generateAccessToken(memberId: String): String = sign(memberId, TokenType.ACCESS, accessTokenExpirationDays)

    private fun generateRefreshToken(memberId: String): String = sign(memberId, TokenType.REFRESH, refreshTokenExpirationDays)

    private fun sign(
        memberId: String,
        tokenType: TokenType,
        expirationDays: Long,
    ): String {
        val now = clock.instant()
        val claimsSet =
            JWTClaimsSet
                .Builder()
                .subject(memberId)
                .claim(TOKEN_TYPE, tokenType.claimValue)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now + Duration.ofDays(expirationDays)))
                .build()

        return SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsSet).apply { sign(signer) }.serialize()
    }

    private fun verify(
        token: String,
        expectedType: TokenType,
    ): String =
        runCatching {
            verifiedClaimsOf(token, expectedType)
        }.getOrElse {
            throw unauthorized(it, expectedType)
        }.subject

    private fun verifiedClaimsOf(
        token: String,
        expectedType: TokenType,
    ): JWTClaimsSet {
        val signedJwt = SignedJWT.parse(token)
        // MACVerifier는 HMAC 계열만 지원하므로 alg를 RS256이나 none으로 바꿔치기한 토큰은 여기서 예외가 된다.
        check(signedJwt.verify(verifier)) { "signature mismatch" }

        val claims = signedJwt.jwtClaimsSet
        check(claims.getStringClaim(TOKEN_TYPE) == expectedType.claimValue) { "token type mismatch" }
        // Nimbus의 만료 검사기는 내부에서 시스템 시각을 쓰므로 주입한 clock을 무시한다. 그래서 직접 비교한다.
        val expiresAt = claims.expirationTime ?: error("missing expiration time")
        if (clock.instant() >= expiresAt.toInstant()) throw ExpiredTokenException()
        check(!claims.subject.isNullOrBlank()) { "missing subject" }

        return claims
    }

    /** 어느 검사에서 떨어졌는지 응답에 담으면 토큰 위조에 힌트가 되므로 사유는 로그에만 남기고 401로만 응답합니다. */
    private fun unauthorized(
        cause: Throwable,
        tokenType: TokenType,
    ): BaseException {
        // 만료는 재로그인하지 않은 클라이언트에서 정상적으로 발생하지만, 서명·종류 불일치는
        // 위조 시도 신호라 운영 기본 로그 레벨(INFO)에서도 보여야 한다.
        if (cause is ExpiredTokenException) {
            logger.debug { "Expired ${tokenType.claimValue}" }
        } else {
            logger.warn(cause) { "Rejected ${tokenType.claimValue}" }
        }

        return BaseException(ErrorCode.UNAUTHORIZED, "${tokenType.label} 검증에 실패했습니다")
    }
}
