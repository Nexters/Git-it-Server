package com.nexters.gitit.application

import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.notification.NotificationSender
import com.nexters.gitit.domain.notification.QuizResultNotification
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.stereotype.Service

/**
 * 문제 생성이 끝났음을 그 저장소를 학습 중인 회원들에게 알립니다.
 *
 * 저장소에서 회원으로 가는 길은 `Project`뿐입니다 — `QuizRepo`는 누가 자기를 학습하는지 모릅니다.
 * 그래서 저장소 하나에 회원이 여럿이고, 알림에 실을 프로젝트 id는 회원마다 다릅니다.
 */
@Service
class NotifyQuizResult(
    private val quizRepoRepository: QuizRepoRepository,
    private val projectRepository: ProjectRepository,
    private val memberRepository: MemberRepository,
    private val notificationSender: NotificationSender,
) {
    /**
     * 푸시 콜이 프로젝트 수만큼 나갑니다. [NotificationSender.send]는 토큰 여러 개에 메시지 하나라,
     * `projectId`가 회원마다 다른 이상 한 번으로 묶을 수 없습니다. 한 저장소의 학습자가 수백 명이 되면
     * 그때 포트에 "토큰별 data"를 넣습니다 — 지금은 루프가 더 쌉니다.
     */
    operator fun invoke(command: Command) {
        // 생성이 끝난 뒤 삭제된 저장소라면 알릴 것이 없다.
        val quizRepo = quizRepoRepository.findById(command.quizRepoId) ?: return
        val projects = projectRepository.findAllByQuizRepoId(quizRepo.id)
        if (projects.isEmpty()) return

        val notification = QuizResultNotification.from(quizRepo.status)
        val tokens =
            memberRepository
                .findAllByIds(projects.map { it.memberId })
                .mapNotNull { member -> member.deviceInfo?.deviceToken?.let { member.id to it } }
                .toMap()

        projects.forEach { project ->
            // 푸시 권한을 거부한 회원은 토큰이 없다. 알림이 유일한 결과 통지가 아니라(화면에서 상태를 읽는다) 그냥 건너뛴다.
            val token = tokens[project.memberId] ?: return@forEach

            notificationSender.send(listOf(token), notification.message(project.id))
        }
    }

    /** 저장소 상태와 알릴 대상은 전부 도큐먼트에서 다시 읽으므로 id 하나만 받습니다. */
    data class Command(
        val quizRepoId: String,
    )
}
