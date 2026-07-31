package com.nexters.gitit.domain.auth

interface JwtProvider {
    fun generateToken(memberId: String): JwtToken

    /**
     * accessToken의 서명·만료·토큰 종류를 검증하고 memberId를 반환합니다.
     *
     * refreshToken을 넘기면 실패합니다. 두 토큰은 만료 기간만 다른 같은 형식이라
     * 종류를 확인하지 않으면 refreshToken으로 API를 호출할 수 있습니다.
     *
     * @throws com.nexters.gitit.domain.exception.BaseException 검증 실패 시 UNAUTHORIZED
     */
    fun verifyAccessToken(accessToken: String): String
}
