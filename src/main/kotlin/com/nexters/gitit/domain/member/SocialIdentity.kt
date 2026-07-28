package com.nexters.gitit.domain.member

/**
 * 회원이 어느 provider의 누구인지를 가리키는 값.
 *
 * [socialId]는 해당 provider 안에서만 유일해 단독으로는 회원을 특정할 수 없습니다.
 * 두 값을 떼어 쓰지 못하도록 묶어둔 것이 이 타입의 존재 이유입니다.
 */
data class SocialIdentity(
    val socialId: String,
    val socialType: SocialType,
)
