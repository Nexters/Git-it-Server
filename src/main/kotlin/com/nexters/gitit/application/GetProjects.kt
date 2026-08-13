package com.nexters.gitit.application

import com.nexters.gitit.domain.problem.ProblemRepository
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetProjects(
    private val projectRepository: ProjectRepository,
    private val problemRepository: ProblemRepository,
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
        val totalCount = problemRepository.countByProjectId(project.id)
        val answeredCount = problemRepository.countByProjectIdAndAnsweredAtIsNotNull(project.id)
        val overallProgressPercent = if (totalCount == 0L) 0 else (answeredCount * 100 / totalCount).toInt()

        val lastAnswered = problemRepository.findFirstByProjectIdAndAnsweredAtIsNotNullOrderByAnsweredAtDesc(project.id)
        // 다 풀었는지와 무관하게 항상 "다음 번호" 문제로 이동. 마지막 문제 다음은 1번으로 되돌아감.
        val nextOrder =
            when {
                lastAnswered == null -> 1
                lastAnswered.order >= totalCount -> 1
                else -> lastAnswered.order + 1
            }
        val nextProblem = problemRepository.findFirstByProjectIdAndOrder(project.id, nextOrder)
        val currentSet = project.sets.firstOrNull { it.setId == nextProblem?.setId }

        return ProjectItem(
            projectId = project.id,
            repositoryImageUrl = project.repositoryImageUrl,
            repositoryName = project.repositoryName,
            techStack = project.techStack,
            currentSetLabel = currentSet?.label.orEmpty(),
            currentSetTitle = currentSet?.title.orEmpty(),
            nextProblemId = nextProblem?.id,
            overallProgressPercent = overallProgressPercent,
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
