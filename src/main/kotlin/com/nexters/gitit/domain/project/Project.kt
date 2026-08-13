package com.nexters.gitit.domain.project

import com.nexters.gitit.domain.common.BaseEntity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "projects")
@CompoundIndex(name = "idx_member_created", def = "{'memberId': 1, 'createdAt': 1}")
class Project(
    val memberId: String,
    val repositoryImageUrl: String?,
    val repositoryName: String,
    val techStack: List<String>,
    val sets: List<LearningSet>,
) : BaseEntity()
