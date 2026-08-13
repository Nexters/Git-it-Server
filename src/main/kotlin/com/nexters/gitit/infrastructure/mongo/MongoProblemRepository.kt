package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.problem.Problem
import com.nexters.gitit.domain.problem.ProblemRepository
import org.springframework.stereotype.Repository

@Repository
class MongoProblemRepository(
    private val problemRepository: SpringDataProblemRepository,
) : ProblemRepository {
    override fun countByProjectId(projectId: String): Long = problemRepository.countByProjectId(projectId)

    override fun countByProjectIdAndAnsweredAtIsNotNull(projectId: String): Long =
        problemRepository.countByProjectIdAndAnsweredAtIsNotNull(projectId)

    override fun findFirstByProjectIdAndAnsweredAtIsNotNullOrderByAnsweredAtDesc(projectId: String): Problem? =
        problemRepository.findFirstByProjectIdAndAnsweredAtIsNotNullOrderByAnsweredAtDesc(projectId)

    override fun findFirstByProjectIdAndOrder(
        projectId: String,
        order: Int,
    ): Problem? = problemRepository.findFirstByProjectIdAndOrder(projectId, order)
}
