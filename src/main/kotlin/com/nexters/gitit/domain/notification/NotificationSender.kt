package com.nexters.gitit.domain.notification

/**
 * 여러 기기에 같은 푸시 알림을 보냅니다.
 *
 * 전송 실패를 예외로 올리지 않는 것이 이 포트의 계약입니다 — 알림을 못 보냈다고 부르는 쪽 작업까지
 * 되돌릴 이유가 없습니다.
 */
interface NotificationSender {
    fun send(
        deviceTokens: List<String>,
        message: NotificationMessage,
    )
}
