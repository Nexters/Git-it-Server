package com.nexters.gitit.domain.member

/**
 * 푸시 알림을 보낼 대상 기기의 정보.
 *
 * [deviceType]을 enum이 아닌 String으로 둔 것은, 클라이언트가 보내는 값을 서버가 재해석하지 않고
 * 그대로 보관하기로 했기 때문입니다. 지원 기기가 늘 때 서버 배포 없이도 값이 흘러들어옵니다.
 *
 * [deviceToken]은 사용자가 푸시 권한을 거부하면 발급되지 않으므로 nullable입니다.
 * 즉 이 값이 없는 기기는 알림 대상에서 빠질 뿐 등록 자체는 정상입니다.
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceType: String,
    val appVersion: String,
    val osVersion: String,
    val deviceToken: String?,
)
