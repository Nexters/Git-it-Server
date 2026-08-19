package com.nexters.gitit.application.project

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.stereotype.Service

/**
 * 회원이 북마크한 문제를 프로젝트별 필터와 함께 조회합니다.
 *
 * [availableProjects]는 [Command.projectId] 필터와 무관하게 북마크가 하나라도 있는 프로젝트 전부입니다.
 * 필터로 걸러진 상태에서도 칩 목록 자체는 그대로 유지돼야 하기 때문입니다.
 */
@Service
class GetBookmarkedQuestions(
    private val projectRepository: ProjectRepository,
    private val quizRepoRepository: QuizRepoRepository,
) {
    operator fun invoke(command: Command): Result {
        val bookmarkedProjects = projectRepository.findAllByMemberId(command.memberId).filter { it.hasBookmark() }
        val quizReposById = quizRepoRepository.findAllByIds(bookmarkedProjects.map { it.quizRepoId }).associateBy { it.id }

        // 가리키는 저장소가 없는 프로젝트는 여기서 빠집니다. 데이터 정합성이 깨진 것인데 예외도 로그도 없이 사라집니다.
        val learningTargets = bookmarkedProjects.mapNotNull { project -> quizReposById[project.quizRepoId]?.let { project to it } }

        val bookmarks =
            learningTargets
                .filter { (project, _) -> command.projectId == null || project.id == command.projectId }
                .flatMap { (project, quizRepo) -> bookmarksOf(project, quizRepo) }

        return Result(
            totalCount = bookmarks.size,
            availableProjects = learningTargets.map { (project, quizRepo) -> Result.AvailableProject(project.id, quizRepo.name) },
            bookmarks = bookmarks,
        )
    }

    private fun bookmarksOf(
        project: Project,
        quizRepo: QuizRepo,
    ): List<Result.BookmarkedQuestion> =
        quizRepo.learningSets.flatMapIndexed { setIndex, set ->
            set.questionsOf(project.quizLevel).mapIndexedNotNull { position, question ->
                if (!project.isBookmarked(question.id)) return@mapIndexedNotNull null

                Result.BookmarkedQuestion(
                    projectId = project.id,
                    projectName = quizRepo.name,
                    setId = set.id,
                    setLabel = "Set ${setIndex + 1}",
                    problemNumber = position + 1,
                    questionId = question.id,
                    question = question.text,
                )
            }
        }

    data class Command(
        val memberId: String,
        val projectId: String?,
    )

    data class Result(
        val totalCount: Int,
        val availableProjects: List<AvailableProject>,
        val bookmarks: List<BookmarkedQuestion>,
    ) {
        /** 북마크 화면의 프로젝트 필터 칩. 필터가 걸린 [bookmarks]에서는 뽑을 수 없어 따로 싣습니다. */
        data class AvailableProject(
            val projectId: String,
            val projectName: String,
        )

        data class BookmarkedQuestion(
            val projectId: String,
            val projectName: String,
            val setId: String,
            val setLabel: String,
            val problemNumber: Int,
            val questionId: String,
            val question: String,
        )
    }
}
