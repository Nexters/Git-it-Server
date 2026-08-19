package com.nexters.gitit.application.project

import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.stereotype.Service

@Service
class DeleteMemberProjects(
    private val projectRepository: ProjectRepository,
) {
    /** 회원 한 명의 Project(학습 진도·답변·북마크)를 하드 삭제합니다. QuizRepo는 공용 자원이라 건드리지 않습니다. */
    operator fun invoke(command: Command) {
        projectRepository.deleteAllByMemberId(command.memberId)
    }

    data class Command(
        val memberId: String,
    )
}
