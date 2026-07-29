package com.nexters.gitit.domain.member

import com.nexters.gitit.domain.common.BaseEntity
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "members")
@CompoundIndex(
    name = "uk_social_identity",
    def = "{'socialIdentity.socialId': 1, 'socialIdentity.socialType': 1}",
    unique = true,
)
class Member(
    val socialIdentity: SocialIdentity,
    email: String?,
    position: Position? = null,
    careerLevel: CareerLevel? = null,
) : BaseEntity() {
    var email: String? = email
        private set

    // TBD 큐레이션 플로우가 정해지지 않았기에 아래 값들은 미정
    var position: Position? = position
        private set

    var careerLevel: CareerLevel? = careerLevel
        private set

    fun isCurated(): Boolean = position != null && careerLevel != null
}
