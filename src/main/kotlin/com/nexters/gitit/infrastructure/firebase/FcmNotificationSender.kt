package com.nexters.gitit.infrastructure.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.nexters.gitit.domain.notification.NotificationMessage
import com.nexters.gitit.domain.notification.NotificationSender
import com.nexters.gitit.infrastructure.firebase.FcmNotificationSender.Companion.MULTICAST_TOKEN_LIMIT
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * iOS도 FCM이 APNs로 중계해 기기 종류로 갈라지지 않습니다.
 *
 * 실패는 로그로만 남깁니다 — 지운 앱의 토큰이 조용히 죽어 있어 실패가 정상 범위입니다.
 */
class FcmNotificationSender(
    private val firebaseMessaging: FirebaseMessaging,
) : NotificationSender {
    /** 상한을 넘겨 보내면 그 묶음이 통째로 거절되므로 [MULTICAST_TOKEN_LIMIT]개씩 나눠 보냅니다. */
    override fun send(
        deviceTokens: List<String>,
        message: NotificationMessage,
    ) {
        val notification =
            Notification
                .builder()
                .setTitle(message.title)
                .setBody(message.body)
                .build()

        // 빈 목록이면 반복이 안 돌아 따로 거르지 않는다.
        deviceTokens.chunked(MULTICAST_TOKEN_LIMIT).forEach { tokens ->
            sendChunk(
                MulticastMessage
                    .builder()
                    .setNotification(notification)
                    .putAllData(message.data)
                    .addAllTokens(tokens)
                    .build(),
            )
        }
    }

    private fun sendChunk(message: MulticastMessage) {
        try {
            val response = firebaseMessaging.sendEachForMulticast(message)
            if (response.failureCount > 0) {
                // 실패 건수만 남기면 토큰이 죽은 건지 설정이 틀린 건지 구분이 안 돼, 사유 코드를 종류별로 센다.
                val reasons =
                    response.responses
                        .mapNotNull { it.exception?.messagingErrorCode }
                        .groupingBy { it }
                        .eachCount()
                logger.warn { "Push delivery failed for ${response.failureCount}/${response.responses.size} devices: $reasons" }
            }
        } catch (e: FirebaseMessagingException) {
            logger.warn(e) { "Push send call failed: ${e.messagingErrorCode}" }
        }
    }

    companion object {
        // FCM이 정한 상한. 줄이는 건 되지만 늘리면 거절된다.
        private const val MULTICAST_TOKEN_LIMIT = 500
    }
}
