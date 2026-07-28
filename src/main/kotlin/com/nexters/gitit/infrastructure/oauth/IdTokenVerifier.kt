package com.nexters.gitit.infrastructure.oauth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import java.net.URI

/**
 * OIDC provider가 발급한 ID 토큰을 검증한다.
 *
 * Google과 Apple 모두 검증 절차가 같고 issuer, 공개키 위치, audience만 다르므로
 * provider별로 클래스를 나누지 않고 이 값들을 생성자로 받는다.
 */
class IdTokenVerifier(
    issuer: String,
    jwkSetUrl: String,
    audience: String,
) {
    private val jwtProcessor =
        DefaultJWTProcessor<SecurityContext>().apply {
            // 공개키는 첫 검증 때 지연 조회된 뒤 캐싱된다. 생성 시점에는 네트워크를 타지 않는다.
            val jwkSource = JWKSourceBuilder.create<SecurityContext>(URI(jwkSetUrl).toURL()).retrying(true).build()

            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)
            jwtClaimsSetVerifier =
                DefaultJWTClaimsVerifier(
                    JWTClaimsSet
                        .Builder()
                        .issuer(issuer)
                        .audience(audience)
                        .build(),
                    // 이 클레임들이 없으면 검증 자체가 성립하지 않으므로 필수로 요구한다.
                    setOf(SUBJECT, ISSUED_AT, EXPIRATION_TIME),
                )
        }

    fun verify(idToken: String): JWTClaimsSet =
        runCatching {
            jwtProcessor.process(JWTParser.parse(idToken), null)
        }.getOrElse {
            // TBD: 공통 예외 체계가 생기면 401로 매핑되는 인증 예외로 교체
            throw IllegalStateException("ID 토큰 검증에 실패했습니다.", it)
        }

    companion object {
        private const val SUBJECT = "sub"
        private const val ISSUED_AT = "iat"
        private const val EXPIRATION_TIME = "exp"
    }
}
