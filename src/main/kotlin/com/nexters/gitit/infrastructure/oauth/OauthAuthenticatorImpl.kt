package com.nexters.gitit.infrastructure.oauth

import com.nexters.gitit.domain.auth.OauthAuthenticator
import com.nexters.gitit.domain.auth.OauthCredential
import com.nexters.gitit.domain.auth.SocialAccount

class OauthAuthenticatorImpl : OauthAuthenticator {
    override fun authenticate(credential: OauthCredential): SocialAccount =
        when (credential) {
            is OauthCredential.Google -> TODO("구글 로그인 구현")
            is OauthCredential.Apple -> TODO("애플 로그인 구현")
        }
}
