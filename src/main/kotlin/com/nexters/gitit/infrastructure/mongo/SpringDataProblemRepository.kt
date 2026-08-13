package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.problem.Problem
import org.springframework.data.mongodb.repository.MongoRepository

interface SpringDataProblemRepository : MongoRepository<Problem, String> {
    fun countByProjectId(projectId: String): Long

    fun countByProjectIdAndAnsweredAtIsNotNull(projectId: String): Long

    fun findFirstByProjectIdAndAnsweredAtIsNotNullOrderByAnsweredAtDesc(projectId: String): Problem?

    fun findFirstByProjectIdAndOrder(
        projectId: String,
        order: Int,
    ): Problem?
}
