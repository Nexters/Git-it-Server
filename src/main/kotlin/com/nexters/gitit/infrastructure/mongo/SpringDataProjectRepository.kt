package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.project.Project
import org.springframework.data.mongodb.repository.MongoRepository

interface SpringDataProjectRepository : MongoRepository<Project, String> {
    fun findByMemberIdAndQuizRepoIdAndDeletedAtIsNull(
        memberId: String,
        quizRepoId: String,
    ): Project?

    fun findAllByQuizRepoIdAndDeletedAtIsNull(quizRepoId: String): List<Project>

    fun findByIdAndDeletedAtIsNull(id: String): Project?

    fun findAllByMemberIdAndDeletedAtIsNull(memberId: String): List<Project>
}
