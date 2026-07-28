package com.nexters.gitit.domain.member

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

// TBD: createdAt, deletedAt, updatedAt 관리에 대한 논의
@Document(collection = "members")
@CompoundIndex(
    name = "uk_social_identity",
    def = "{'socialIdentity.socialId': 1, 'socialIdentity.socialType': 1}",
    unique = true,
)
class Member(
    @Id val id: String = ObjectId().toString(),
    val socialIdentity: SocialIdentity,
    email: String?,
) {
    var email: String? = email
        private set
}
