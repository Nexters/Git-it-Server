package com.nexters.gitit.domain.notification

import com.nexters.gitit.domain.quizrepo.QuizRepoStatus

/**
 * 문제 생성 결과를 알리는 문구 한 벌. 가르는 기준은 **받은 사람이 할 수 있는 일**입니다.
 *
 * [REJECTED]는 이 저장소로는 문제를 못 만든다는 판정이라 재시도를 권하지 않고, [FAILED]는 사고라
 * 다시 하면 성공할 수 있습니다.
 *
 * 거절 사유(`QuizRepo.rejectedReason`)는 싣지 않습니다. 내부 에러 코드이고, 화면에서 다시 읽습니다.
 */
enum class QuizResultNotification(
    private val type: String,
    private val title: String,
    private val body: String,
) {
    READY("QUIZ_READY", "프로젝트 준비 완료", "새 문제가 도착했어요"),
    REJECTED("QUIZ_REJECTED", "문제를 만들 수 없는 저장소예요", "다른 저장소로 등록해 주세요"),
    FAILED("QUIZ_FAILED", "문제를 만들지 못했어요", "잠시 후 다시 시도해 주세요"),
    ;

    /**
     * [type]은 앱과 맞춰 둔 문자열이라 enum 이름과 따로 답니다. 값이 달라지면 클라이언트가 조용히
     * 못 알아듣습니다.
     *
     * 문구는 모두에게 같지만 [projectId]는 회원마다 다릅니다. 눌렀을 때 열 화면이 회원별 프로젝트입니다.
     */
    fun message(projectId: String): NotificationMessage =
        NotificationMessage(
            title = title,
            body = body,
            data = mapOf("type" to type, "projectId" to projectId),
        )

    companion object {
        /** 끝났다는 이벤트를 받고도 상태가 완료·거절이 아니면 사고로 끝난 것입니다. */
        fun from(status: QuizRepoStatus): QuizResultNotification =
            when (status) {
                QuizRepoStatus.COMPLETED -> READY
                QuizRepoStatus.REJECTED -> REJECTED
                else -> FAILED
            }
    }
}
