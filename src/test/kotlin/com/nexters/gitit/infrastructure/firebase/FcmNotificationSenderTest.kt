package com.nexters.gitit.infrastructure.firebase

import com.google.firebase.messaging.MulticastMessage
import com.nexters.gitit.domain.notification.NotificationMessage
import com.nexters.gitit.infrastructure.firebase.FcmNotificationSenderTest.Companion.DEVICE_TOKEN
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * 자격증명·프로젝트 설정·토큰이 실제로 맞물리는지는 진짜로 보내 봐야 압니다. 목으로 막으면 우리가 짠
 * 빌더 호출만 확인하게 됩니다.
 *
 * [DEVICE_TOKEN]에 확인할 기기의 토큰을 붙여 넣고 `./gradlew networkTest`로 돌립니다. 비워 두면 건너뜁니다 —
 * 남의 기계에서 붉은불이 되면 안 됩니다. 붙여 넣은 토큰은 커밋하지 않습니다.
 */
@Tag("network")
@EnabledIfEnvironmentVariable(named = "GCP_CREDENTIALS_BASE64", matches = ".+")
class FcmNotificationSenderTest {
    private val configuration = FirebaseConfiguration()
    private val firebaseMessaging =
        configuration.firebaseMessaging(
            configuration.firebaseApp(System.getenv("GCP_CREDENTIALS_BASE64")),
        )
    private val sender = FcmNotificationSender(firebaseMessaging)

    /**
     * 어댑터가 실패를 삼키므로 통과했다고 기기에 도착한 것은 아닙니다. 기계가 판정할 수 있는 데까지만
     * 검사하고(토큰이 살아 있는지), 알림이 실제로 뜨는지는 기기를 보고 확인합니다.
     */
    @Test
    fun `기기 토큰으로 테스트 푸시를 보낸다`() {
        assumeTrue(DEVICE_TOKEN.isNotBlank())

        sender.send(
            listOf(DEVICE_TOKEN),
            NotificationMessage(
                title = "Git-it 테스트 푸시",
                body = "이 알림이 보이면 FCM 설정이 정상입니다",
                data = mapOf("type" to "TEST"),
            ),
        )

        // 실제 발송 없이 검증만 하는 호출이라 기기에 알림이 두 번 뜨지 않는다.
        val validation = firebaseMessaging.sendEachForMulticast(MulticastMessage.builder().addToken(DEVICE_TOKEN).build(), true)

        // successCount만 보면 "1이 아니라 0"까지만 알게 된다. 토큰이 틀린 건지 프로젝트가 다른 건지는 FCM이 준 사유에 있다.
        val rejection = validation.responses.single().exception
        withClue({ "FCM이 거절: ${rejection?.messagingErrorCode} ${rejection?.message}" }) {
            validation.successCount shouldBe 1
        }
    }

    companion object {
        private const val DEVICE_TOKEN = ""
    }
}
