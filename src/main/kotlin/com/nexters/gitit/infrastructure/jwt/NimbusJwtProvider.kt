package com.nexters.gitit.infrastructure.jwt

import com.nexters.gitit.domain.auth.JwtProvider
import com.nexters.gitit.domain.auth.JwtToken
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.Date

@Component
class NimbusJwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access_token.expiration_days}") private val accessTokenExpirationDays: Long,
    @Value("\${jwt.refresh_token.expiration_days}") private val refreshTokenExpirationDays: Long,
    private val clock: Clock,
) : JwtProvider {
    private val signer = MACSigner(secret.toByteArray())

    override fun generateToken(memberId: String): JwtToken =
        JwtToken(
            accessToken = generateAccessToken(memberId),
            refreshToken = generateRefreshToken(memberId),
        )

    private fun generateAccessToken(memberId: String): String = sign(memberId, ACCESS_TOKEN, accessTokenExpirationDays)

    private fun generateRefreshToken(memberId: String): String = sign(memberId, REFRESH_TOKEN, refreshTokenExpirationDays)

    private fun sign(
        memberId: String,
        tokenType: String,
        expirationDays: Long,
    ): String {
        val now = clock.instant()
        val claimsSet =
            JWTClaimsSet
                .Builder()
                .subject(memberId)
                .claim(TOKEN_TYPE, tokenType)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now + Duration.ofDays(expirationDays)))
                .build()

        return SignedJWT(JWSHeader(JWSAlgorithm.HS256), claimsSet)
            .apply { sign(signer) }
            .serialize()
    }

    companion object {
        private const val ACCESS_TOKEN = "accessToken"
        private const val REFRESH_TOKEN = "refreshToken"
        private const val TOKEN_TYPE = "type"
    }
}
