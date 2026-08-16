package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.project.QuizLevel
import com.nexters.gitit.domain.quizrepo.GithubRepository
import com.nexters.gitit.domain.quizrepo.GithubRepositoryResolver
import com.nexters.gitit.domain.quizrepo.QuizGenerationRequested
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class RegisterProject(
    private val quizRepoRepository: QuizRepoRepository,
    private val projectRepository: ProjectRepository,
    private val githubRepositoryResolver: GithubRepositoryResolver,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * GitHub 저장소를 받아 회원의 프로젝트로 등록합니다. 다른 회원이 이미 등록해 둔 저장소면 문제 세트를 새로
     * 만들지 않고 그대로 함께 씁니다.
     *
     * 문제 생성을 기다리지 않고 이벤트로 넘기므로, 갓 등록한 저장소의 결과 상태는 아직 완료가 아닙니다.
     *
     * 등록할 수 없는 저장소는 결과가 아니라 예외로 알립니다. GitHub에 없으면 도큐먼트를 남기지 않는데, 식별자를
     * 얻지 못해 유니크 키를 만들 수 없고 오타 URL마다 레코드가 쌓이기 때문입니다. 문제를 낼 수 없다고 이미
     * 판정된 저장소는 그때 기록해 둔 사유를 그대로 던집니다.
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
     * 넘긴 객체가 그대로 돌아왔을 때만 이번 요청이 저장소를 만든 것이고, 그때만 문제 생성을 겁니다.
     * 이미 있던 저장소까지 이벤트를 내면 같은 저장소에 수 분짜리 생성 작업이 중복으로 돕니다.
     */
    private fun registerQuizRepo(
        repository: GithubRepository,
        githubRepoUrl: String,
    ): QuizRepo {
        val requested =
            QuizRepo(
                githubRepoId = repository.id,
                githubRepoUrl = githubRepoUrl,
                name = repository.name,
                ownerImageUrl = repository.ownerImageUrl,
                starCount = repository.starCount,
                techStacks = repository.techStacks,
            )
        val quizRepo = quizRepoRepository.saveIfAbsent(requested)
        if (quizRepo.id == requested.id) {
            eventPublisher.publishEvent(QuizGenerationRequested(quizRepo.id))
        }

        return quizRepo
    }

    data class Command(
        val memberId: String,
        val githubRepoUrl: String,
        val quizLevel: QuizLevel,
    )

    data class Result(
        val projectId: String,
        val status: QuizRepoStatus,
    ) {
        constructor(project: Project, status: QuizRepoStatus) : this(project.id, status)
    }
}
