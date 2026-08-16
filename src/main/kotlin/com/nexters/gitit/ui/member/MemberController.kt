package com.nexters.gitit.ui.member

import com.nexters.gitit.application.CurateMember
import com.nexters.gitit.application.GetMemberProfile
import com.nexters.gitit.application.RegisterDeviceInfo
import com.nexters.gitit.application.UpdateMemberCareerLevel
import com.nexters.gitit.application.UpdateMemberPosition
import com.nexters.gitit.application.UpdateNotificationSettings
import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.common.LoginMember
import com.nexters.gitit.ui.member.dto.CareerLevelRequest
import com.nexters.gitit.ui.member.dto.CurationRequest
import com.nexters.gitit.ui.member.dto.DeviceInfoRequest
import com.nexters.gitit.ui.member.dto.MemberProfileResponse
import com.nexters.gitit.ui.member.dto.NotificationSettingsRequest
import com.nexters.gitit.ui.member.dto.PositionRequest
import jakarta.validation.Valid
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members", produces = [APPLICATION_JSON_VALUE])
class MemberController(
    private val registerDeviceInfo: RegisterDeviceInfo,
    private val curateMember: CurateMember,
    private val updateMemberPosition: UpdateMemberPosition,
    private val updateMemberCareerLevel: UpdateMemberCareerLevel,
    private val updateNotificationSettings: UpdateNotificationSettings,
    private val getMemberProfile: GetMemberProfile,
) : MemberControllerDocs {
    @GetMapping("/me")
    override fun getMemberProfile(
        @LoginMember memberId: String,
    ): ApiResponse<MemberProfileResponse> {
        val result = getMemberProfile(GetMemberProfile.Command(memberId))
        return ApiResponse.success(MemberProfileResponse.from(result))
    }

    // 같은 기기를 다시 등록해도 새 리소스가 생기지 않고 기존 값을 덮어쓰므로 201이 아닌 200으로 응답합니다.
    @PostMapping("/me/device")
    override fun registerDeviceInfo(
        @LoginMember memberId: String,
        @Valid @RequestBody request: DeviceInfoRequest,
    ): ApiResponse<Unit> {
        registerDeviceInfo(request.toCommand(memberId))

        return ApiResponse.success()
    }

    // 다시 호출하면 이전 선택을 덮어쓰므로 201이 아닌 200으로 응답합니다.
    @PostMapping("/me/curation")
    override fun curateMember(
        @LoginMember memberId: String,
        @Valid @RequestBody request: CurationRequest,
    ): ApiResponse<Unit> {
        curateMember(request.toCommand(memberId))

        return ApiResponse.success()
    }

    // 설정 화면에서 개발 분야만 따로 바꾸는 용도라 curation과 별개 엔드포인트로 둡니다.
    @PostMapping("/me/position")
    override fun updatePosition(
        @LoginMember memberId: String,
        @Valid @RequestBody request: PositionRequest,
    ): ApiResponse<Unit> {
        updateMemberPosition(request.toCommand(memberId))

        return ApiResponse.success()
    }

    @PostMapping("/me/career-level")
    override fun updateCareerLevel(
        @LoginMember memberId: String,
        @Valid @RequestBody request: CareerLevelRequest,
    ): ApiResponse<Unit> {
        updateMemberCareerLevel(request.toCommand(memberId))

        return ApiResponse.success()
    }

    // 다시 호출하면 이전 설정을 덮어쓰므로 201이 아닌 200으로 응답합니다.
    @PostMapping("/me/notification-settings")
    override fun updateNotificationSettings(
        @LoginMember memberId: String,
        @Valid @RequestBody request: NotificationSettingsRequest,
    ): ApiResponse<Unit> {
        updateNotificationSettings(request.toCommand(memberId))

        return ApiResponse.success()
    }
}
