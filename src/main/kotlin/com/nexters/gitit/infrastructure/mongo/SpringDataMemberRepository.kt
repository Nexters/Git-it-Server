package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.member.Member
import com.nexters.gitit.domain.member.SocialIdentity
import org.springframework.data.mongodb.repository.MongoRepository

interface SpringDataMemberRepository : MongoRepository<Member, String> {
    fun findByIdAndDeletedAtIsNull(id: String): Member?

    fun findBySocialIdentityAndDeletedAtIsNull(socialIdentity: SocialIdentity): Member?
}
