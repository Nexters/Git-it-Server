package com.nexters.gitit.infrastructure.oauth

import com.nexters.gitit.domain.auth.OauthCredential
import com.nexters.gitit.domain.auth.SocialAccount
import com.nexters.gitit.domain.member.SocialIdentity
import com.nexters.gitit.domain.member.SocialType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GoogleOauthVerifier(
    @Value("\${oauth.google.client-id}") clientId: String,
) {
    private val idTokenVerifier = IdTokenVerifier(ISSUER, JWK_SET_URL, clientId)

    fun verify(credential: OauthCredential.Google): SocialAccount {
        val claims = idTokenVerifier.verify(credential.idToken)

        return SocialAccount(
            socialIdentity = SocialIdentity(claims.subject, SocialType.GOOGLE),
            email = claims.getStringClaim(EMAIL),
        )
    }

    companion object {
        private const val ISSUER = "https://accounts.google.com"
        private const val JWK_SET_URL = "https://www.googleapis.com/oauth2/v3/certs"
        private const val EMAIL = "email"
    }
}
