package com.nexters.gitit.application.project

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.GithubRepository
import com.nexters.gitit.domain.quizrepo.GithubRepositoryResolver
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class RegisterProject(
    private val quizRepoRepository: QuizRepoRepository,
    private val projectRepository: ProjectRepository,
    private val githubRepositoryResolver: GithubRepositoryResolver,
    private val clock: Clock,
) {
    /**
     * GitHub 저장소를 받아 회원의 프로젝트로 등록합니다. 다른 회원이 이미 등록해 둔 저장소면 문제 세트를 새로
     * 만들지 않고 그대로 함께 씁니다.
     *
     * 문제 생성을 기다리지 않고 대기줄에 세워 두고 끝내므로, 갓 등록한 저장소의 결과 상태는 아직 완료가 아닙니다.
     *
     * 등록할 수 없는 저장소는 [BaseException]으로 알립니다. GitHub에 없는 주소면 도큐먼트를 남기지 않고,
     * 문제를 낼 수 없다고 이미 판정된 저장소는 그때 기록해 둔 사유를 그대로 던집니다.
     */
    operator fun invoke(command: Command): Result {
        val repository =
            githubRepositoryResolver.resolve(command.githubRepoUrl)
                ?: throw BaseException(ErrorCode.INVALID_INPUT, "유효하지 않은 GitHub 저장소입니다")

        val quizRepo = registerQuizRepo(repository, command.githubRepoUrl)
        if (quizRepo.status == QuizRepoStatus.REJECTED) {
            // reject()가 상태와 사유를 함께 세팅하므로 사유가 빌 수 없지만, 타입이 nullable이라 기본값을 둔다.
            throw BaseException(quizRepo.rejectedReason ?: ErrorCode.INVALID_INPUT)
        }

        val project = projectRepository.saveIfAbsent(Project(command.memberId, quizRepo.id, command.quizLevel))

        return Result(project, quizRepo.status)
    }

    /**
     * 저장이 곧 생성 요청입니다. READY로 저장된 저장소를 스케줄러가 대기줄에서 집어 갑니다.
     *
     * 이미 있던 저장소는 [QuizRepoRepository.saveIfAbsent]가 저장 없이 그대로 돌려주므로,
     * 같은 저장소가 두 번 줄에 서지 않습니다.
     */
    private fun registerQuizRepo(
        repository: GithubRepository,
        githubRepoUrl: String,
    ): QuizRepo =
        quizRepoRepository.saveIfAbsent(
            QuizRepo(
                githubRepoId = repository.id,
                githubRepoUrl = githubRepoUrl,
                name = repository.name,
                ownerImageUrl = repository.ownerImageUrl,
                starCount = repository.starCount,
                techStacks = repository.techStacks,
                registeredAt = Instant.now(clock),
            ),
        )

    data class Command(
        val memberId: String,
        val githubRepoUrl: String,
        val quizLevel: Depth,
    )

    data class Result(
        val projectId: String,
        val status: QuizRepoStatus,
    ) {
        constructor(project: Project, status: QuizRepoStatus) : this(project.id, status)
    }
}
