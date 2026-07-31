package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.member.Member
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.member.SocialIdentity
import org.springframework.stereotype.Repository

@Repository
class MongoMemberRepository(
    private val memberRepository: SpringDataMemberRepository,
) : MemberRepository {
    override fun save(member: Member): Member = memberRepository.save(member)

    override fun findBySocialIdentity(socialIdentity: SocialIdentity): Member? =
        memberRepository.findBySocialIdentityAndDeletedAtIsNull(socialIdentity)
}
