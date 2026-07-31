package com.nexters.gitit.infrastructure.oauth

import com.nexters.gitit.domain.auth.OauthAuthenticator
import com.nexters.gitit.domain.auth.OauthCredential
import com.nexters.gitit.domain.auth.SocialAccount
import org.springframework.stereotype.Component

@Component
class DelegatingOauthAuthenticator(
    private val appleOauthVerifier: AppleOauthVerifier,
    private val googleOauthVerifier: GoogleOauthVerifier,
) : OauthAuthenticator {
    override fun authenticate(credential: OauthCredential): SocialAccount =
        when (credential) {
            is OauthCredential.Google -> googleOauthVerifier.verify(credential)
            is OauthCredential.Apple -> appleOauthVerifier.verify(credential)
        }
}
