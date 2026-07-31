package com.nexters.gitit.infrastructure.oauth

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimNames
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.proc.BadJWTException
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * OIDC provider가 발급한 ID 토큰을 검증한다.
 *
 * Google과 Apple 모두 검증 절차가 같고 issuer, 공개키 위치, audience만 다르므로
 * provider별로 클래스를 나누지 않고 이 값들을 생성자로 받는다.
 *
 * issuer는 같은 provider가 표기를 달리해 발급하기도 하므로 후보 집합으로 받는다.
 */
class IdTokenVerifier(
    private val issuers: Set<String>,
    jwkSetUrl: String,
    audience: String,
) {
    private val jwtProcessor =
        DefaultJWTProcessor<SecurityContext>().apply {
            val jwkSource = JWKSourceBuilder.create<SecurityContext>(URI(jwkSetUrl).toURL()).retrying(true).build()

            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)
            // issuer는 값 비교를 여기서 못 한다. exactMatchClaims는 단일 값 완전 일치라
            // 같은 provider가 표기만 달리해 발급한 정상 토큰까지 떨어뜨린다. 존재 여부만 요구하고 값은 아래에서 본다.
            jwtClaimsSetVerifier =
                DefaultJWTClaimsVerifier(
                    audience,
                    null,
                    setOf(
                        JWTClaimNames.SUBJECT,
                        JWTClaimNames.ISSUED_AT,
                        JWTClaimNames.EXPIRATION_TIME,
                        JWTClaimNames.ISSUER,
                    ),
                )
        }

    fun verify(idToken: String): JWTClaimsSet {
        // 서명·만료·issuer 중 무엇이 틀렸는지 알려주면 토큰 위조에 힌트가 되므로 401로만 응답한다.
        val claims =
            runCatching {
                jwtProcessor.process(JWTParser.parse(idToken), null)
            }.getOrElse {
                throw unauthorized(it)
            }

        if (claims.issuer !in issuers) {
            // 다른 provider가 발급한 토큰을 들고 온 것이라 정상 흐름에서는 나올 수 없다.
            logger.warn { "ID token issuer rejected: ${claims.issuer}" }
            throw BaseException(ErrorCode.UNAUTHORIZED, "ID 토큰 검증에 실패했습니다")
        }

        return claims
    }

    private fun unauthorized(cause: Throwable): BaseException {
        // 만료나 audience 불일치(BadJWTException)는 클라이언트 사정으로 정상 발생하지만,
        // 서명 검증 실패나 JWKS 조회 실패는 위조 시도이거나 우리 쪽 장애라
        // 운영 기본 로그 레벨(INFO)에서도 보여야 한다.
        if (cause is BadJWTException) {
            logger.debug(cause) { "ID token claims rejected" }
        } else {
            logger.warn(cause) { "ID token verification failed" }
        }

        return BaseException(ErrorCode.UNAUTHORIZED, "ID 토큰 검증에 실패했습니다")
    }
}
