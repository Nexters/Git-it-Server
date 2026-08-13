package com.nexters.gitit.domain.problem

import com.nexters.gitit.domain.common.BaseEntity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * 문제 생성/풀이 기능이 아직 구현되지 않아, 프로젝트 목록의 "다음 문제" 계산에 필요한
 * 최소 필드만 정의합니다. 질문/보기 등 나머지 필드는 해당 기능 구현 시 추가됩니다.
 */
@Document(collection = "problems")
@CompoundIndex(name = "idx_project_order", def = "{'projectId': 1, 'order': 1}", unique = true)
class Problem(
    val projectId: String,
    val setId: String,
    val order: Int,
    var answeredAt: Instant? = null,
) : BaseEntity()
