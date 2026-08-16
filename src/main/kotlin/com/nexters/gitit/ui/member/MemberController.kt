package com.nexters.gitit.ui.member

import com.nexters.gitit.application.CurateMember
import com.nexters.gitit.application.RegisterDeviceInfo
import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.common.LoginMember
import com.nexters.gitit.ui.member.dto.CurationRequest
import com.nexters.gitit.ui.member.dto.DeviceInfoRequest
import jakarta.validation.Valid
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members", produces = [APPLICATION_JSON_VALUE])
class MemberController(
    private val registerDeviceInfo: RegisterDeviceInfo,
    private val curateMember: CurateMember,
) : MemberControllerDocs {
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
}
