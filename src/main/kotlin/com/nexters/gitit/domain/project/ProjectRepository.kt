package com.nexters.gitit.domain.project

interface ProjectRepository {
    fun save(project: Project): Project

    fun findByMemberIdAndQuizRepoId(
        memberId: String,
        quizRepoId: String,
    ): Project?
}
