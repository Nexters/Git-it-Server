package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Repository

@Repository
class MongoProjectRepository(
    private val projectRepository: SpringDataProjectRepository,
) : ProjectRepository {
    override fun findByMemberIdAndDeletedAtIsNull(
        memberId: String,
        pageable: Pageable,
    ): Slice<Project> = projectRepository.findByMemberIdAndDeletedAtIsNull(memberId, pageable)
}
