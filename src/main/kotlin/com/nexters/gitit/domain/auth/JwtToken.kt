package com.nexters.gitit.domain.auth

data class JwtToken(
    val accessToken: String,
    val refreshToken: String,
)
