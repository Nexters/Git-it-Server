package com.nexters.gitit.domain.member

import org.springframework.data.mongodb.repository.MongoRepository

interface MemberRepository : MongoRepository<Member, String> {
    fun findBySocialIdentity(socialIdentity: SocialIdentity): Member?
}
