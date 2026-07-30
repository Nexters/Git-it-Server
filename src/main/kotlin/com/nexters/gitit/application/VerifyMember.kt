package com.nexters.gitit.application

import com.nexters.gitit.domain.auth.JwtProvider
import org.springframework.stereotype.Service

@Service
class VerifyMember(
    private val jwtProvider: JwtProvider,
) {
    operator fun invoke(accessToken: String): String = jwtProvider.verifyAccessToken(accessToken)
}
