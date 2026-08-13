package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.stereotype.Service

@Service
class GetProjectRepositoryUrl(
    private val projectRepository: ProjectRepository,
) {
    operator fun invoke(command: Command): Result {
        val project =
            projectRepository.findByIdAndMemberIdAndDeletedAtIsNull(command.projectId, command.memberId)
                ?: throw BaseException(ErrorCode.NOT_FOUND, "프로젝트를 찾을 수 없습니다")

        return Result(repositoryUrl = project.repositoryUrl)
    }

    data class Command(
        val memberId: String,
        val projectId: String,
    )

    data class Result(
        val repositoryUrl: String?,
    )
}
