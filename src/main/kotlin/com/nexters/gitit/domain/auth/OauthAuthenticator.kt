package com.nexters.gitit.domain.auth

/**
 * 소셜 로그인 자격 증명을 검증하고, 신뢰할 수 있는 사용자 식별 정보를 얻는 진입점.
 *
 * 지원 provider가 늘어나도 호출부가 provider별 검증 방식(토큰 서명 검증, 공개키 조회 등)을
 * 알 필요가 없도록 격리하기 위해 존재합니다.
 */
interface OauthAuthenticator {
    fun authenticate(credential: OauthCredential): SocialAccount
}
