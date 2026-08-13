package com.nexters.gitit.domain.project

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface ProjectRepository {
    fun findByMemberIdAndDeletedAtIsNull(
        memberId: String,
        pageable: Pageable,
    ): Slice<Project>

    fun findByIdAndMemberIdAndDeletedAtIsNull(
        id: String,
        memberId: String,
    ): Project?

    fun save(project: Project): Project
}
