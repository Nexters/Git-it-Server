package com.nexters.gitit.event

import com.nexters.gitit.application.notification.NotifyQuizResult
import com.nexters.gitit.domain.quizrepo.QuizGenerationFinished
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * 퀴즈 도메인의 이벤트 수신 지점. 어떤 이벤트가 어떤 유스케이스로 이어지는지 이 파일만 보면 됩니다.
 *
 * 리스너를 유스케이스마다 흩어 두면 발행자와 수신자를 잇는 지도가 코드 어디에도 남지 않습니다.
 * 유스케이스는 이벤트를 모르고, 이벤트는 유스케이스를 모르며, 배선은 여기서만 합니다.
 */
@Component
class QuizEventHandler(
    private val notifyQuizResult: NotifyQuizResult,
) {
    /**
     * 이 이벤트는 문제 생성의 finally에서, 예외를 다시 던지는 경로에서도 나옵니다.
     * 동기로 받으면 알림 발송이 그 스택 위에서 돌아 문제 생성 스레드를 더 붙잡습니다.
     */
    @Async
    @EventListener
    fun handle(event: QuizGenerationFinished) {
        notifyQuizResult(NotifyQuizResult.Command(event.quizRepoId))
    }
}
