package com.nexters.gitit.domain.project

import com.nexters.gitit.domain.common.BaseEntity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

/**
 * repositoryUrl은 프로젝트 생성 기능 구현 전이라 아직 아무도 채우지 않아 nullable입니다.
 * 생성 요청 body에 이미 들어오는 값을 그대로 저장하면 됩니다.
 */
@Document(collection = "projects")
@CompoundIndex(name = "idx_member_created", def = "{'memberId': 1, 'createdAt': 1}")
class Project(
    val memberId: String,
    val repositoryUrl: String?,
    val repositoryImageUrl: String?,
    val repositoryName: String,
    val techStack: List<String>,
    val sets: List<LearningSet>,
) : BaseEntity()
