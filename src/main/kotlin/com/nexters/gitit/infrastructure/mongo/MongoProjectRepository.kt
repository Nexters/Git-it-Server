package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository

@Repository
class MongoProjectRepository(
    private val projectRepository: SpringDataProjectRepository,
) : ProjectRepository {
    override fun saveIfAbsent(project: Project): Project =
        try {
            projectRepository.save(project)
        } catch (e: DuplicateKeyException) {
            projectRepository.findByMemberIdAndQuizRepoIdAndDeletedAtIsNull(project.memberId, project.quizRepoId) ?: throw e
        }

    override fun save(project: Project): Project = projectRepository.save(project)

    override fun findAllByQuizRepoId(quizRepoId: String): List<Project> =
        projectRepository.findAllByQuizRepoIdAndDeletedAtIsNull(quizRepoId)

    override fun findById(id: String): Project? = projectRepository.findByIdAndDeletedAtIsNull(id)

    override fun findAllByMemberId(memberId: String): List<Project> = projectRepository.findAllByMemberIdAndDeletedAtIsNull(memberId)

    override fun deleteAllByMemberId(memberId: String) = projectRepository.deleteAllByMemberId(memberId)
}
