package com.nexters.gitit.application

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetProjects(
    private val projectRepository: ProjectRepository,
    private val projectProgressCalculator: ProjectProgressCalculator,
) {
    // ponytail: 프로젝트당 여러 번 조회하는 N+1 구조. 목록 규모가 유저 개인 프로젝트 수준이라 지금은
    // 문제없지만, 페이지당 프로젝트 수가 커지면 집계 파이프라인으로 묶는 걸 고려.
    operator fun invoke(command: Command): Result {
        val slice = projectRepository.findByMemberIdAndDeletedAtIsNull(command.memberId, command.pageable)
        return Result(
            items = slice.content.map { toItem(it) },
            hasNext = slice.hasNext(),
        )
    }

    private fun toItem(project: Project): ProjectItem {
        val progress = projectProgressCalculator.calculate(project.id)
        val currentSet = project.sets.firstOrNull { it.setId == progress.nextProblem?.setId }

        return ProjectItem(
            projectId = project.id,
            repositoryImageUrl = project.repositoryImageUrl,
            repositoryName = project.repositoryName,
            techStack = project.techStack,
            currentSetLabel = currentSet?.label.orEmpty(),
            currentSetTitle = currentSet?.title.orEmpty(),
            nextProblemId = progress.nextProblem?.id,
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
        val repositoryImageUrl: String?,
        val repositoryName: String,
        val techStack: List<String>,
        val currentSetLabel: String,
        val currentSetTitle: String,
        val nextProblemId: String?,
        val overallProgressPercent: Int,
    )
}
