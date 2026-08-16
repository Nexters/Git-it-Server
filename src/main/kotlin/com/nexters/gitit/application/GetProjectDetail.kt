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
        val project =
            projectRepository.findByIdAndMemberIdAndDeletedAtIsNull(command.projectId, command.memberId)
                ?: throw BaseException(ErrorCode.NOT_FOUND, "프로젝트를 찾을 수 없습니다")
        // quizRepoId가 가리키는 QuizRepo가 없는 것도 데이터 정합성이 깨진 경우라, 같은 404로 취급한다.
        val quizRepo =
            quizRepoRepository.findById(project.quizRepoId)
                ?: throw BaseException(ErrorCode.NOT_FOUND, "프로젝트를 찾을 수 없습니다")

        val progress = ProjectProgress.calculate(project, quizRepo)
        val depth = Depth.valueOf(project.quizLevel.name)

        val sets =
            quizRepo.learningSets.mapIndexed { index, set ->
                SetItem(
                    setId = set.id,
                    label = "Set ${index + 1}",
                    title = set.title,
                    problemCount = set.questions[depth]?.size ?: 0,
                    completedCount = progress.completedCountsBySet.getOrElse(index) { 0 },
                )
            }

        return Result(
            repositoryUrl = quizRepo.githubRepoUrl,
            repositoryName = repositoryNameOf(quizRepo.githubRepoUrl),
            repositoryImageUrl = quizRepo.repositoryImageUrl,
            starCount = quizRepo.starCount,
            techStack = quizRepo.techStack,
            overallProgressPercent = progress.overallProgressPercent,
            nextProblemId = progress.nextQuestionId,
            sets = sets,
        )
    }

    data class Command(
        val memberId: String,
        val projectId: String,
    )

    data class Result(
        val repositoryUrl: String,
        val repositoryName: String,
        val repositoryImageUrl: String?,
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
