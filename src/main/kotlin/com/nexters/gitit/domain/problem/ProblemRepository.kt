package com.nexters.gitit.domain.problem

interface ProblemRepository {
    fun countByProjectId(projectId: String): Long

    fun countByProjectIdAndAnsweredAtIsNotNull(projectId: String): Long

    fun findFirstByProjectIdAndAnsweredAtIsNotNullOrderByAnsweredAtDesc(projectId: String): Problem?

    fun findFirstByProjectIdAndOrder(
        projectId: String,
        order: Int,
    ): Problem?

    fun countByProjectIdAndSetIdAndAnsweredAtIsNotNull(
        projectId: String,
        setId: String,
    ): Long
}
