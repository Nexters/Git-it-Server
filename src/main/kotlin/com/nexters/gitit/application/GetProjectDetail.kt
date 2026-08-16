package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.problem.ProblemRepository
import com.nexters.gitit.domain.project.LearningSet
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.stereotype.Service

@Service
class GetProjectDetail(
    private val projectRepository: ProjectRepository,
    private val problemRepository: ProblemRepository,
    private val projectProgressCalculator: ProjectProgressCalculator,
) {
    operator fun invoke(command: Command): Result {
        val project =
            projectRepository.findByIdAndMemberIdAndDeletedAtIsNull(command.projectId, command.memberId)
                ?: throw BaseException(ErrorCode.NOT_FOUND, "프로젝트를 찾을 수 없습니다")

        val progress = projectProgressCalculator.calculate(project.id)

        return Result(
            repositoryImageUrl = project.repositoryImageUrl,
            repositoryName = project.repositoryName,
            starCount = project.starCount,
            techStack = project.techStack,
            overallProgressPercent = progress.overallProgressPercent,
            nextProblemId = progress.nextProblem?.id,
            sets = project.sets.map { toSetItem(project.id, it) },
        )
    }

    private fun toSetItem(
        projectId: String,
        set: LearningSet,
    ): SetItem {
        val completedCount = problemRepository.countByProjectIdAndSetIdAndAnsweredAtIsNotNull(projectId, set.setId)
        return SetItem(
            setId = set.setId,
            label = set.label,
            title = set.title,
            problemCount = set.problemCount,
            completedCount = completedCount.toInt(),
        )
    }

    data class Command(
        val memberId: String,
        val projectId: String,
    )

    data class Result(
        val repositoryImageUrl: String?,
        val repositoryName: String,
        val starCount: Long?,
        val techStack: List<String>,
        val overallProgressPercent: Int,
        val nextProblemId: String?,
        val sets: List<SetItem>,
    )

    data class SetItem(
        val setId: String,
        val label: String,
        val title: String,
        val problemCount: Int,
        val completedCount: Int,
    )
}
