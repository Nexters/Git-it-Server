package com.nexters.gitit.domain.member

interface MemberRepository {
    fun findBySocialIdentity(socialIdentity: SocialIdentity): Member?

    fun save(member: Member): Member
}
