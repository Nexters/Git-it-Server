package com.nexters.gitit.domain.member

/**
 * 회원 문서가 지워졌음을 알립니다. 회원에 매달린 데이터를 지우는 일은 이 이벤트를 받아서 합니다.
 *
 * 탈퇴 응답을 붙잡지 않으려고 비동기로 받으므로, 정리 도중 실패하면 주인 없는 데이터가 남습니다.
 */
data class MemberWithdrawn(
    val memberId: String,
)
