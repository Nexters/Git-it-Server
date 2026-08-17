package com.nexters.gitit.application

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.project.QuizLevel
import com.nexters.gitit.domain.quizrepo.Depth
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
        val myProjects = projectRepository.findAllByMemberId(command.memberId)
        val bookmarkedProjects = myProjects.filter { it.bookmarkedQuestionIds.isNotEmpty() }

        val quizReposById = bookmarkedProjects.mapNotNull { quizRepoRepository.findById(it.quizRepoId) }.associateBy { it.id }

        val availableProjects =
            bookmarkedProjects.mapNotNull { project ->
                quizReposById[project.quizRepoId]?.let { AvailableProject(projectId = project.id, projectName = it.name) }
            }

        val targetProjects =
            if (command.projectId == null) bookmarkedProjects else bookmarkedProjects.filter { it.id == command.projectId }

        val bookmarks =
            targetProjects.flatMap { project ->
                quizReposById[project.quizRepoId]?.let { bookmarksOf(project, it) }.orEmpty()
            }

        return Result(totalCount = bookmarks.size, availableProjects = availableProjects, bookmarks = bookmarks)
    }

    private fun bookmarksOf(
        project: Project,
        quizRepo: QuizRepo,
    ): List<BookmarkedQuestion> {
        val depth = project.quizLevel.toDepth()

        return quizRepo.learningSets.flatMapIndexed { setIndex, set ->
            set.questionsOf(depth).mapIndexedNotNull { position, question ->
                if (question.id !in project.bookmarkedQuestionIds) return@mapIndexedNotNull null

                BookmarkedQuestion(
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
    }

    // 이름이 같아도 enum이 둘이라 valueOf로 잇지 않는다. 한쪽에 레벨이 늘면 컴파일이 깨져야 옮겨 적는 것을 잊지 않는다.
    private fun QuizLevel.toDepth() =
        when (this) {
            QuizLevel.L1 -> Depth.L1
            QuizLevel.L2 -> Depth.L2
            QuizLevel.L3 -> Depth.L3
        }

    data class Command(
        val memberId: String,
        val projectId: String?,
    )

    data class Result(
        val totalCount: Int,
        val availableProjects: List<AvailableProject>,
        val bookmarks: List<BookmarkedQuestion>,
    )

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
