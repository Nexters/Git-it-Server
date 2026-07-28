package com.nexters.gitit.domain.member

import java.util.UUID

// TBD: createdAt, deletedAt, updatedAt 관리에 대한 논의
class Member(
    val id: String = UUID.randomUUID().toString(),
    val socialIdentity: SocialIdentity,
    email: String?,
) {
    var email: String? = email
        private set
}
