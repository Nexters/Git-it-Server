package com.nexters.gitit.ui.member.dto

import com.nexters.gitit.application.RegisterDeviceInfo
import com.nexters.gitit.domain.member.DeviceInfo
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * deviceType은 값 집합을 검증하지 않습니다. 서버가 이 값으로 분기하지 않고 보관만 하기 때문에,
 * 지원 기기가 늘 때 서버를 먼저 배포해야 하는 순서 의존을 만들지 않으려는 선택입니다.
 */
data class DeviceInfoRequest(
    @field:Schema(description = "기기 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
    @field:NotBlank(message = "deviceId는 필수입니다")
    val deviceId: String,
    @field:Schema(description = "디바이스 타입 - \"ios\", \"android\", \"web\"", example = "ios")
    @field:NotBlank(message = "deviceType은 필수입니다")
    val deviceType: String,
    @field:Schema(description = "앱 버전", example = "1.0.0")
    @field:NotBlank(message = "appVersion은 필수입니다")
    val appVersion: String,
    @field:Schema(description = "OS 버전", example = "18.2")
    @field:NotBlank(message = "osVersion은 필수입니다")
    val osVersion: String,
    @field:Schema(description = "푸시 발송에 쓰는 기기 토큰. 사용자가 알림 권한을 거부했다면 생략합니다")
    val deviceToken: String?,
) {
    fun toCommand(memberId: String) =
        RegisterDeviceInfo.Command(
            memberId = memberId,
            deviceInfo =
                DeviceInfo(
                    deviceId = deviceId,
                    deviceType = deviceType,
                    appVersion = appVersion,
                    osVersion = osVersion,
                    deviceToken = deviceToken,
                ),
        )
}
