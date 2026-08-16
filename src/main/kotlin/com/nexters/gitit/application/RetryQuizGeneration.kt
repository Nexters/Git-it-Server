package com.nexters.gitit.application

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.QuizGenerationRequested
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class RetryQuizGeneration(
    private val projectRepository: ProjectRepository,
    private val quizRepoRepository: QuizRepoRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * 사고로 멈춘 저장소의 문제 생성을 다시 겁니다.
     *
     * 한 번 호출이 한 번 시도입니다. 또 실패하면 부르는 쪽이 다시 겁니다.
     *
     * 등록이 아니라 이 길이 따로 필요한 것은, [QuizGenerationRequested]가 [RegisterProject]에서
     * **저장소를 새로 만든 요청에만** 나가기 때문입니다. 같은 레포를 다시 등록해도 이벤트가 나가지 않습니다.
     *
     * 저장소는 여러 회원이 나눠 쓰지만 재시도는 프로젝트로 받습니다 — 요청한 사람이 그 저장소를 학습하고
     * 있는지 확인할 길이 프로젝트뿐입니다.
     */
    operator fun invoke(command: Command) {
        val project = projectRepository.findById(command.projectId) ?: throw BaseException(ErrorCode.PROJECT_NOT_FOUND)
        project.requireOwnedBy(command.memberId)

        // 프로젝트가 가리키는 저장소가 없는 것은 잘못된 요청이 아니라 데이터가 깨진 것이라, 404로 덮으면 원인이 묻힌다.
        val quizRepo =
            quizRepoRepository.findById(project.quizRepoId)
                ?: error("프로젝트가 가리키는 저장소가 없습니다: quizRepoId=${project.quizRepoId}")

        // 재시도해도 되는 상태인지는 도큐먼트가 판정한다. 여기서 다시 검사하면 규칙이 두 벌이 된다.
        quizRepo.retry()
        // 이벤트보다 저장이 먼저다. 뒤집으면 비동기 리스너가 아직 FAILED인 도큐먼트를 읽는다.
        quizRepoRepository.save(quizRepo)
        eventPublisher.publishEvent(QuizGenerationRequested(quizRepo.id))
    }

    data class Command(
        val memberId: String,
        val projectId: String,
    )
}
