package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class DeleteProject(
    private val projectRepository: ProjectRepository,
    private val clock: Clock,
) {
    /**
     * 본인 소유가 아니거나 이미 삭제된 프로젝트는 존재 여부를 노출하지 않기 위해 동일하게 NOT_FOUND로 응답합니다.
     * QuizRepo는 여러 회원이 공유하므로 함께 지우지 않는다 - Project만 소프트 삭제한다.
     */
    operator fun invoke(command: Command) {
        val project =
            projectRepository.findByIdAndMemberIdAndDeletedAtIsNull(command.projectId, command.memberId)
                ?: throw BaseException(ErrorCode.NOT_FOUND, "프로젝트를 찾을 수 없습니다")

        project.delete(clock)
        projectRepository.save(project)
    }

    data class Command(
        val memberId: String,
        val projectId: String,
    )
}
