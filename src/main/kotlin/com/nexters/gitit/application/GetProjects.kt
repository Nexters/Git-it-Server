package com.nexters.gitit.application

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetProjects(
    private val projectRepository: ProjectRepository,
    private val quizRepoRepository: QuizRepoRepository,
) {
    // ponytail: 프로젝트당 QuizRepo를 하나씩 더 조회하는 N+1 구조. 목록 규모가 유저 개인 프로젝트
    // 수준이라 지금은 문제없지만, 페이지당 프로젝트 수가 커지면 quizRepoId로 한 번에 묶어 조회하는 걸 고려.
    operator fun invoke(command: Command): Result {
        val slice = projectRepository.findAllByMemberIdAndDeletedAtIsNull(command.memberId, command.pageable)
        return Result(
            items = slice.content.mapNotNull { toItem(it) },
            hasNext = slice.hasNext(),
        )
    }

    // quizRepoId가 가리키는 QuizRepo가 없는 건 데이터 정합성이 깨진 경우라 정상적으로는 없어야 하지만,
    // 목록 전체를 500으로 죽이는 것보다 그 항목만 빼고 보여주는 편이 낫다.
    private fun toItem(project: Project): ProjectItem? {
        val quizRepo = quizRepoRepository.findById(project.quizRepoId) ?: return null
        val progress = ProjectProgress.calculate(project, quizRepo)
        val currentSet = progress.nextSetIndex?.let { quizRepo.learningSets.getOrNull(it) }

        return ProjectItem(
            projectId = project.id,
            repositoryName = quizRepo.name,
            repositoryImageUrl = quizRepo.ownerImageUrl,
            techStack = quizRepo.techStacks,
            currentSetLabel = progress.nextSetIndex?.let { "Set ${it + 1}" }.orEmpty(),
            currentSetTitle = currentSet?.title.orEmpty(),
            nextProblemId = progress.nextQuestionId,
            overallProgressPercent = progress.overallProgressPercent,
        )
    }

    data class Command(
        val memberId: String,
        val pageable: Pageable,
    )

    data class Result(
        val items: List<ProjectItem>,
        val hasNext: Boolean,
    )

    data class ProjectItem(
        val projectId: String,
        val repositoryName: String,
        val repositoryImageUrl: String,
        val techStack: List<String>,
        val currentSetLabel: String,
        val currentSetTitle: String,
        val nextProblemId: String?,
        val overallProgressPercent: Int,
    )
}
