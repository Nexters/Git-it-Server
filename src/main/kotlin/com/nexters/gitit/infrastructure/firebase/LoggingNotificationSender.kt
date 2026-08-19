package com.nexters.gitit.infrastructure.firebase

import com.nexters.gitit.domain.notification.NotificationMessage
import com.nexters.gitit.domain.notification.NotificationSender
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

/**
 * 자격증명이 없는 환경에서 쓰는 대역. 보낼 뻔한 것을 로그로만 남깁니다.
 *
 * [FirebaseConfiguration]이 통째로 꺼지면 `NotificationSender` 빈이 아예 없어져, 이 포트를 주입받는
 * 유스케이스 때문에 컨텍스트가 기동하지 않습니다. 테스트와 자격증명 없는 로컬이 그 경우입니다.
 */
class LoggingNotificationSender : NotificationSender {
    override fun send(
        deviceTokens: List<String>,
        message: NotificationMessage,
    ) {
        logger.info { "Push skipped (no FCM credentials): '${message.title}' to ${deviceTokens.size} devices" }
    }
}

/**
 * [FirebaseConfiguration]과 정확히 반대 조건이라, 프로퍼티 값이 무엇이든 둘 중 한쪽만 켜집니다 —
 * 프로퍼티가 없거나 비어 있으면 이 대역이 뜹니다.
 */
@Configuration
@ConditionalOnExpression("'\${firebase.credentials-base64:}' == ''")
class NoFirebaseConfiguration {
    @Bean
    fun loggingNotificationSender(): NotificationSender = LoggingNotificationSender()
}
