package com.nexters.gitit.application

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import org.springframework.stereotype.Service

@Service
class GetProjects(
    private val projectRepository: ProjectRepository,
    private val quizRepoRepository: QuizRepoRepository,
) {
    // ponytail: 페이지를 나누지 않고 전부 내려주는 것은 목록 규모가 유저 개인 프로젝트 수준이라는
    // 전제에 기대고 있다. 한 회원이 수백 개를 학습하게 되면 조회도 응답도 같이 커진다.
    operator fun invoke(command: Command): Result {
        val projects = projectRepository.findAllByMemberId(command.memberId).sortedBy { it.createdAt }
        val quizReposById = quizRepoRepository.findAllByIds(projects.map { it.quizRepoId }).associateBy { it.id }

        return Result(
            items = projects.mapNotNull { project -> learnableQuizRepoOf(project, quizReposById)?.let { toItem(project, it) } },
        )
    }

    /**
     * 지금 풀 수 있는 저장소만 돌려줍니다. 아니면 null이고, 그 프로젝트는 목록에서 빠집니다.
     *
     * 빼는 경우가 둘입니다. 문제 생성이 끝나지 않았으면 낼 문제가 없어 빈 카드가 됩니다.
     * quizRepoId가 가리키는 것이 아예 없으면 데이터 정합성이 깨진 것인데, 예외도 로그도 없이
     * 그 항목만 사라집니다.
     */
    private fun learnableQuizRepoOf(
        project: Project,
        quizReposById: Map<String, QuizRepo>,
    ): QuizRepo? = quizReposById[project.quizRepoId]?.takeIf { it.status == QuizRepoStatus.COMPLETED }

    private fun toItem(
        project: Project,
        quizRepo: QuizRepo,
    ): ProjectItem {
        val progress = ProjectProgress.calculate(project, quizRepo)
        val currentSet = progress.nextSetIndex?.let { quizRepo.learningSets.getOrNull(it) }

        return ProjectItem(
            projectId = project.id,
            repositoryName = quizRepo.name,
            repositoryImageUrl = quizRepo.ownerImageUrl,
            techStack = quizRepo.techStacks,
            currentSetLabel = progress.nextSetIndex?.let { "Set ${it + 1}" }.orEmpty(),
            currentSetTitle = currentSet?.title.orEmpty(),
            nextSetId = currentSet?.id,
            nextQuestionId = progress.nextQuestionId,
            overallProgressPercent = progress.overallProgressPercent,
        )
    }

    data class Command(
        val memberId: String,
    )

    data class Result(
        val items: List<ProjectItem>,
    )

    data class ProjectItem(
        val projectId: String,
        val repositoryName: String,
        val repositoryImageUrl: String,
        val techStack: List<String>,
        val currentSetLabel: String,
        val currentSetTitle: String,
        val nextSetId: String?,
        val nextQuestionId: String?,
        val overallProgressPercent: Int,
    )
}
