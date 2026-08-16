package com.nexters.gitit.domain.notification

/**
 * 알림 한 건의 내용. 도메인이 FCM 타입을 모르게 하려고 우리 타입을 둡니다.
 *
 * [data]는 화면에 안 보이고 앱이 읽는 값(눌렀을 때 갈 곳 등)입니다. 전송 규격상 숫자도 문자열로 넣어야 합니다.
 */
data class NotificationMessage(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
)
