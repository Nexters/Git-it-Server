package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.stereotype.Service

@Service
class BookmarkQuestion(
    private val projectRepository: ProjectRepository,
    private val quizRepoRepository: QuizRepoRepository,
) {
    operator fun invoke(command: Command): Result {
        val project = projectRepository.findById(command.projectId) ?: throw BaseException(ErrorCode.PROJECT_NOT_FOUND)
        project.requireOwnedBy(command.memberId)

        // 프로젝트가 가리키는 저장소가 없는 것은 잘못된 요청이 아니라 데이터가 깨진 것이라, 404로 덮으면 원인이 묻힌다.
        val quizRepo =
            quizRepoRepository.findById(project.quizRepoId)
                ?: error("프로젝트가 가리키는 저장소가 없습니다: quizRepoId=${project.quizRepoId}")
        quizRepo.findQuestion(command.questionId) ?: throw BaseException(ErrorCode.QUESTION_NOT_FOUND)

        project.setBookmarked(command.questionId, command.bookmarked)
        projectRepository.save(project)

        return Result(bookmarked = command.bookmarked)
    }

    data class Command(
        val memberId: String,
        val projectId: String,
        val questionId: String,
        val bookmarked: Boolean,
    )

    data class Result(
        val bookmarked: Boolean,
    )
}
