package com.nexters.gitit.domain.auth

import com.nexters.gitit.domain.member.SocialIdentity

/**
 * provider가 자격 증명 검증을 통해 확인해준 소셜 계정.
 *
 * provider 쪽 계정일 뿐 우리 서비스의 회원(Member)은 아닙니다.
 * 첫 로그인이라면 아직 대응하는 Member가 없을 수 있습니다.
 *
 * Apple은 최초 인가 때만 이메일을 내려주므로 [email]은 재로그인 시 null일 수 있고,
 * 사용자가 이메일 가리기를 선택했다면 릴레이 주소가 담깁니다.
 */
data class SocialAccount(
    val socialIdentity: SocialIdentity,
    val email: String?,
)
