package com.nexters.gitit.domain.member

interface MemberRepository {
    fun save(member: Member): Member

    fun findById(id: String): Member?

    fun findBySocialIdentity(socialIdentity: SocialIdentity): Member?
}
