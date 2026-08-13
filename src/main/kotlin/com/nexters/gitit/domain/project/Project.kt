package com.nexters.gitit.domain.project

import com.nexters.gitit.domain.common.BaseEntity
import com.nexters.gitit.domain.member.Position
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

/**
 * repositoryUrl/techDomain은 프로젝트 생성 기능 구현 전이라 아직 아무도 채우지 않아 nullable입니다.
 * 생성 시 repositoryUrl은 요청 body 값을 그대로, techDomain은 서버가 레포를 분석해 자동으로 채웁니다.
 */
@Document(collection = "projects")
@CompoundIndex(name = "idx_member_created", def = "{'memberId': 1, 'createdAt': 1}")
class Project(
    val memberId: String,
    val repositoryUrl: String?,
    val repositoryImageUrl: String?,
    val repositoryName: String,
    val techStack: List<String>,
    val techDomain: Position?,
    val sets: List<LearningSet>,
) : BaseEntity()
