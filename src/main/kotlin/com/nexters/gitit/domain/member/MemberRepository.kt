package com.nexters.gitit.domain.member

interface MemberRepository {
    fun save(member: Member): Member

    fun findById(id: String): Member?

    /** 없는 id는 결과에서 빠집니다. 개수가 맞는지 확인하려면 부르는 쪽이 셉니다. */
    fun findAllByIds(ids: List<String>): List<Member>

    fun findBySocialIdentity(socialIdentity: SocialIdentity): Member?

    /** 소프트 삭제가 아니라 문서를 실제로 지웁니다. 회원 탈퇴 전용입니다. */
    fun deleteById(id: String)
}
