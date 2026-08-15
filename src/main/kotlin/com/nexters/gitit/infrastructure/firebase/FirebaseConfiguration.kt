package com.nexters.gitit.infrastructure.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.nexters.gitit.domain.notification.NotificationSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Base64

/**
 * FCM 발송에 필요한 빈을 모아 둡니다.
 *
 * 자격증명이 없으면 빈을 아예 만들지 않습니다 — 테스트와 로컬이 푸시 없이도 떠야 해서입니다.
 */
@Configuration
@ConditionalOnProperty("firebase.credentials-base64")
class FirebaseConfiguration {
    /** 두 번 초기화하면 예외라, 컨텍스트가 여러 번 뜨는 테스트를 위해 이미 있는 앱을 씁니다. */
    @Bean
    fun firebaseApp(
        @Value("\${firebase.credentials-base64}") credentialsBase64: String,
    ): FirebaseApp {
        val credentials = GoogleCredentials.fromStream(Base64.getMimeDecoder().decode(credentialsBase64).inputStream())
        val options = FirebaseOptions.builder().setCredentials(credentials).build()

        return FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME } ?: FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)

    /** `@Component`로 두면 자격증명이 없는 환경에서도 스캔에 걸려 주입할 빈을 못 찾고 기동이 깨집니다. */
    @Bean
    fun fcmNotificationSender(firebaseMessaging: FirebaseMessaging): NotificationSender = FcmNotificationSender(firebaseMessaging)
}
