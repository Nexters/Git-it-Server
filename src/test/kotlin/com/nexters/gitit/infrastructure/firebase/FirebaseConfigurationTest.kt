package com.nexters.gitit.infrastructure.firebase

import com.nexters.gitit.domain.notification.NotificationSender
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * 자격증명이 "빈 값"일 때가 함정입니다. `GCP_CREDENTIALS_BASE64`를 채우지 않은 채 export 하면 프로퍼티는
 * 있으면서 값만 비는데, 여기서 두 구성이 엇갈리면 진짜 어댑터가 떠서 빈 자격증명으로 죽습니다.
 *
 * 컨텍스트 전체를 띄우지 않고 두 구성만 올려 조건 평가 결과만 봅니다.
 */
class FirebaseConfigurationTest {
    private val runner =
        ApplicationContextRunner()
            .withUserConfiguration(FirebaseConfiguration::class.java, NoFirebaseConfiguration::class.java)

    @Test
    fun `자격증명이 비어 있으면 로그 대역이 뜬다`() {
        runner.withPropertyValues("firebase.credentials-base64=").run {
            it.getBean(NotificationSender::class.java).shouldBeInstanceOf<LoggingNotificationSender>()
        }
    }

    @Test
    fun `자격증명 설정이 아예 없어도 로그 대역이 뜬다`() {
        runner.run {
            it.getBean(NotificationSender::class.java).shouldBeInstanceOf<LoggingNotificationSender>()
        }
    }
}
