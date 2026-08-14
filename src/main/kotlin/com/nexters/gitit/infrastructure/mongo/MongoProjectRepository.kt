package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.stereotype.Repository

@Repository
class MongoProjectRepository(
    private val projectRepository: SpringDataProjectRepository,
) : ProjectRepository {
    override fun save(project: Project): Project = projectRepository.save(project)

    override fun findByMemberIdAndQuizRepoId(
        memberId: String,
        quizRepoId: String,
    ): Project? = projectRepository.findByMemberIdAndQuizRepoIdAndDeletedAtIsNull(memberId, quizRepoId)
}
