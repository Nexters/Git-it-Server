package com.nexters.gitit.domain.project

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface ProjectRepository {
    fun findByMemberIdAndDeletedAtIsNull(
        memberId: String,
        pageable: Pageable,
    ): Slice<Project>
}
