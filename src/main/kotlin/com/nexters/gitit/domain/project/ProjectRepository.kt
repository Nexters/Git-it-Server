package com.nexters.gitit.domain.project

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface ProjectRepository {
    /**
     * 그 회원이 이미 학습 중인 저장소면 기존 프로젝트를, 아니면 새로 저장한 것을 돌려줍니다.
     *
     * 이미 있을 때 난이도를 덮어쓰지 않는 것이 이 메서드의 계약입니다. 난이도 변경은 등록과 구분되는 별도 행위입니다.
     */
    fun saveIfAbsent(project: Project): Project

    fun save(project: Project): Project

    /** 저장소 하나를 학습 중인 프로젝트 전부. 문제 생성이 끝났을 때 알릴 대상을 찾는 방향입니다. */
    fun findAllByQuizRepoId(quizRepoId: String): List<Project>

    fun findAllByMemberIdAndDeletedAtIsNull(
        memberId: String,
        pageable: Pageable,
    ): Slice<Project>

    fun findByIdAndMemberIdAndDeletedAtIsNull(
        id: String,
        memberId: String,
    ): Project?
}
