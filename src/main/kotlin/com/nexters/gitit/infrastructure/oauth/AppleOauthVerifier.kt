package com.nexters.gitit.infrastructure.oauth

import com.nexters.gitit.domain.auth.OauthCredential
import com.nexters.gitit.domain.auth.SocialAccount
import com.nexters.gitit.domain.member.SocialIdentity
import com.nexters.gitit.domain.member.SocialType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AppleOauthVerifier(
    @Value("\${oauth.apple.client-id}") clientId: String,
) {
    private val idTokenVerifier = IdTokenVerifier(setOf(ISSUER), JWK_SET_URL, clientId)

    fun verify(credential: OauthCredential.Apple): SocialAccount {
        val claims = idTokenVerifier.verify(credential.idToken)

        return SocialAccount(
            socialIdentity = SocialIdentity(claims.subject, SocialType.APPLE),
            // Apple은 최초 인가 때만 이메일을 내려주므로 재로그인 시에는 클레임이 없다.
            email = claims.getStringClaim(EMAIL),
        )
    }

    companion object {
        private const val ISSUER = "https://appleid.apple.com"
        private const val JWK_SET_URL = "https://appleid.apple.com/auth/keys"
        private const val EMAIL = "email"
    }
}
