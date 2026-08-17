package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.stereotype.Service

@Service
class GetProjectDetail(
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

        val progress = ProjectProgress.calculate(project, quizRepo)
        val depth = Depth.valueOf(project.quizLevel.name)

        val sets =
            quizRepo.learningSets.mapIndexed { index, set ->
                SetItem(
                    setId = set.id,
                    label = "Set ${index + 1}",
                    title = set.title,
                    problemCount = set.questionsOf(depth).size,
                    completedCount = progress.completedCountsBySet.getOrElse(index) { 0 },
                )
            }

        return Result(
            projectId = project.id,
            repositoryUrl = quizRepo.githubRepoUrl,
            repositoryName = quizRepo.name,
            repositoryImageUrl = quizRepo.ownerImageUrl,
            starCount = quizRepo.starCount,
            techStack = quizRepo.techStacks,
            overallProgressPercent = progress.overallProgressPercent,
            nextQuestionId = progress.nextQuestionId,
            sets = sets,
        )
    }

    data class Command(
        val memberId: String,
        val projectId: String,
    )

    data class Result(
        val projectId: String,
        val repositoryUrl: String,
        val repositoryName: String,
        val repositoryImageUrl: String,
        val starCount: Int,
        val techStack: List<String>,
        val overallProgressPercent: Int,
        val nextQuestionId: String?,
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
