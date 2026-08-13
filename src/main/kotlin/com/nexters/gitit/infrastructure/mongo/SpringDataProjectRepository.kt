package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.project.Project
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.mongodb.repository.MongoRepository

interface SpringDataProjectRepository : MongoRepository<Project, String> {
    fun findByMemberIdAndDeletedAtIsNull(
        memberId: String,
        pageable: Pageable,
    ): Slice<Project>
}
