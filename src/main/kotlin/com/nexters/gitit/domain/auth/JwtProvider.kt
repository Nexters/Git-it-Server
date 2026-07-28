package com.nexters.gitit.domain.auth

interface JwtProvider {
    fun generateToken(memberId: String): JwtToken
}
