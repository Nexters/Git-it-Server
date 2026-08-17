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
    // ponytail: 프로젝트당 QuizRepo를 하나씩 더 조회하는 N+1 구조. 목록 규모가 유저 개인 프로젝트
    // 수준이라 지금은 문제없지만, 커지면 quizRepoId로 한 번에 묶어 조회하는 걸 고려. 페이지를 나누지
    // 않고 전부 내려주는 것도 같은 전제에 기대고 있다.
    operator fun invoke(command: Command): Result {
        val projects = projectRepository.findAllByMemberId(command.memberId)
        return Result(
            items =
                projects
                    .sortedBy { it.createdAt }
                    .mapNotNull { project -> learnableQuizRepoOf(project)?.let { toItem(project, it) } },
        )
    }

    /**
     * 지금 풀 수 있는 저장소만 돌려줍니다. 아니면 null이고, 그 프로젝트는 목록에서 빠집니다.
     *
     * 빼는 경우가 둘입니다. 문제 생성이 끝나지 않았으면 낼 문제가 없어 빈 카드가 됩니다.
     * quizRepoId가 가리키는 것이 아예 없으면 데이터 정합성이 깨진 것인데, 예외도 로그도 없이
     * 그 항목만 사라집니다.
     */
    private fun learnableQuizRepoOf(project: Project): QuizRepo? =
        quizRepoRepository.findById(project.quizRepoId)?.takeIf { it.status == QuizRepoStatus.COMPLETED }

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
