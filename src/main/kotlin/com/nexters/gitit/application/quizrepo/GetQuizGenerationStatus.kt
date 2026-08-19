package com.nexters.gitit.application.quizrepo

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import org.springframework.stereotype.Service

@Service
class GetQuizGenerationStatus(
    private val projectRepository: ProjectRepository,
    private val quizRepoRepository: QuizRepoRepository,
) {
    /**
     * 프로젝트가 보고 있는 저장소의 문제 생성 상태만 돌려줍니다. 상세 조회와 달리 세트와 진행률을 계산하지
     * 않아, 생성이 끝나기를 기다리며 되묻는 쪽이 부담 없이 부를 수 있습니다.
     *
     * 점유(STARTED)를 READY로 접는 것은 응답 DTO가 합니다. 여기서는 저장된 상태를 그대로 냅니다.
     */
    operator fun invoke(command: Command): Result {
        val project = projectRepository.findById(command.projectId) ?: throw BaseException(ErrorCode.PROJECT_NOT_FOUND)
        project.requireOwnedBy(command.memberId)

        // 프로젝트가 가리키는 저장소가 없는 것은 잘못된 요청이 아니라 데이터가 깨진 것이라, 404로 덮으면 원인이 묻힌다.
        val quizRepo =
            quizRepoRepository.findById(project.quizRepoId)
                ?: error("프로젝트가 가리키는 저장소가 없습니다: quizRepoId=${project.quizRepoId}")

        return Result(quizRepo.status)
    }

    data class Command(
        val memberId: String,
        val projectId: String,
    )

    data class Result(
        val status: QuizRepoStatus,
    )
}
