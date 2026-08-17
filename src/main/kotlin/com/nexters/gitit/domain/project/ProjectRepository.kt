package com.nexters.gitit.domain.project

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

    /** 살아 있는 프로젝트 하나. 없거나 이미 삭제됐으면 null입니다. */
    fun findById(id: String): Project?

    /** 그 회원의 살아 있는 프로젝트 전부. */
    fun findAllByMemberId(memberId: String): List<Project>

    /** 소프트 삭제와 무관하게 그 회원의 프로젝트를 전부 실제로 지웁니다. 회원 탈퇴 전용입니다. */
    fun deleteAllByMemberId(memberId: String)
}
