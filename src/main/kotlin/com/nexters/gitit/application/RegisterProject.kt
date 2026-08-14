package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.project.QuizLevel
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
     * GitHub에 없는 저장소는 도큐먼트를 남기지 않고 예외로 끝냅니다 — 식별자를 얻지 못해 유니크 키를 만들 수 없고,
     * 오타 URL마다 레코드가 쌓이기 때문입니다.
     */
    operator fun invoke(command: Command): Result {
        val githubRepoId =
            githubRepositoryResolver.resolve(command.githubRepoUrl)
                ?: throw BaseException(ErrorCode.INVALID_INPUT, "유효하지 않은 GitHub 저장소입니다")

        val quizRepo = quizRepoRepository.findByGithubRepoId(githubRepoId) ?: register(githubRepoId, command.githubRepoUrl)
        if (quizRepo.status == QuizRepoStatus.REJECTED) {
            throw BaseException(quizRepo.rejectedReason ?: ErrorCode.INVALID_INPUT)
        }

        return Result(startProject(command.memberId, quizRepo.id, command.quizLevel), quizRepo.status)
    }

    private fun register(
        githubRepoId: String,
        githubRepoUrl: String,
    ): QuizRepo {
        val quizRepo = quizRepoRepository.save(QuizRepo(githubRepoId = githubRepoId, githubRepoUrl = githubRepoUrl))
        eventPublisher.publishEvent(QuizGenerationRequested(quizRepo.id))

        return quizRepo
    }

    /**
     * 이미 그 회원의 프로젝트면 난이도를 건드리지 않고 기존 것을 그대로 돌려줍니다. 등록은 등록만 하고 난이도 변경은
     * 별도 유스케이스여야, 사용자가 "다시 등록"한 것인지 "난이도를 바꾼" 것인지 호출부에서 구분됩니다.
     */
    private fun startProject(
        memberId: String,
        quizRepoId: String,
        quizLevel: QuizLevel,
    ): Project =
        projectRepository.findByMemberIdAndQuizRepoId(memberId, quizRepoId)
            ?: projectRepository.save(Project(memberId = memberId, quizRepoId = quizRepoId, quizLevel = quizLevel))

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
