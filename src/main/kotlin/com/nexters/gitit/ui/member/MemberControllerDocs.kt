package com.nexters.gitit.ui.member

import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.member.dto.CareerLevelRequest
import com.nexters.gitit.ui.member.dto.CurationRequest
import com.nexters.gitit.ui.member.dto.DeviceInfoRequest
import com.nexters.gitit.ui.member.dto.MemberProfileResponse
import com.nexters.gitit.ui.member.dto.NotificationSettingsRequest
import com.nexters.gitit.ui.member.dto.PositionRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Member", description = "회원 API")
interface MemberControllerDocs {
    @Operation(
        summary = "멤버 정보 조회",
        description =
            "마이페이지 진입 시 필요한 내 프로필 정보와 학습 현황(이번 주/이번 달 푼 문제 수, 연속 학습 일수, " +
                "이번 주 요일별 문제 풀이량)을 조회합니다. nickname·profileImageUrl은 아직 저장하는 값이 없어 응답에 없습니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "조회 성공",
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = MEMBER_NOT_FOUND_EXAMPLE)])],
        ),
    )
    fun getMemberProfile(memberId: String): ApiResponse<MemberProfileResponse>

    @Operation(
        summary = "기기 정보 등록",
        description =
            "푸시 발송 대상이 될 기기 정보를 등록합니다. 회원당 기기 하나만 보관하므로 다시 호출하면 이전 정보를 덮어씁니다. " +
                "앱 버전이나 OS 버전이 바뀌면 다시 호출해야 최신 값이 유지됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "등록 성공",
        ),
        SwaggerApiResponse(
            responseCode = "400",
            description = "필수 값이 비어 있음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = INVALID_INPUT_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = MEMBER_NOT_FOUND_EXAMPLE)])],
        ),
    )
    fun registerDeviceInfo(
        memberId: String,
        request: DeviceInfoRequest,
    ): ApiResponse<Unit>

    @Operation(
        summary = "회원 큐레이션 등록",
        description =
            "로그인 응답의 needsCuration이 true일 때 온보딩에서 받은 관심 분야·실력 수준을 저장합니다. " +
                "다시 호출하면 이전 선택을 덮어씁니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "등록 성공",
        ),
        SwaggerApiResponse(
            responseCode = "400",
            description = "필수 값이 비어 있음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = CURATION_INVALID_INPUT_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = MEMBER_NOT_FOUND_EXAMPLE)])],
        ),
    )
    fun curateMember(
        memberId: String,
        request: CurationRequest,
    ): ApiResponse<Unit>

    @Operation(
        summary = "개발 분야 변경",
        description = "설정 화면에서 개발 분야만 따로 변경합니다. 실력 수준에는 영향을 주지 않습니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "변경 성공",
        ),
        SwaggerApiResponse(
            responseCode = "400",
            description = "필수 값이 비어 있음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = POSITION_INVALID_INPUT_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = MEMBER_NOT_FOUND_EXAMPLE)])],
        ),
    )
    fun updatePosition(
        memberId: String,
        request: PositionRequest,
    ): ApiResponse<Unit>

    @Operation(
        summary = "개발 수준 변경",
        description = "설정 화면에서 개발 수준만 따로 변경합니다. 개발 분야에는 영향을 주지 않습니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "변경 성공",
        ),
        SwaggerApiResponse(
            responseCode = "400",
            description = "필수 값이 비어 있음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = CAREER_LEVEL_INVALID_INPUT_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = MEMBER_NOT_FOUND_EXAMPLE)])],
        ),
    )
    fun updateCareerLevel(
        memberId: String,
        request: CareerLevelRequest,
    ): ApiResponse<Unit>

    @Operation(
        summary = "세트 생성 완료 알림 설정",
        description =
            "학습 세트 생성이 완료됐을 때 푸시 알림을 받을지 설정합니다. 실제 발송을 받으려면 기기 정보 등록으로 " +
                "기기 토큰이 먼저 등록돼 있어야 합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "변경 성공",
        ),
        SwaggerApiResponse(
            responseCode = "400",
            description = "필수 값이 비어 있음",
            content = [
                Content(
                    mediaType = APPLICATION_JSON_VALUE,
                    examples = [ExampleObject(value = NOTIFICATION_SETTINGS_INVALID_INPUT_EXAMPLE)],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = MEMBER_NOT_FOUND_EXAMPLE)])],
        ),
    )
    fun updateNotificationSettings(
        memberId: String,
        request: NotificationSettingsRequest,
    ): ApiResponse<Unit>

    companion object {
        // 401은 OpenApiConfig의 loginMemberSecurityCustomizer가 @LoginMember 파라미터를 보고 자동으로 붙이므로 여기 적지 않습니다.
        private const val INVALID_INPUT_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"잘못된 요청입니다","errors":[{"field":"deviceId","message":"deviceId는 필수입니다"}]}"""

        private const val MEMBER_NOT_FOUND_EXAMPLE =
            """{"success":false,"data":null,"code":"MEMBER-001","message":"회원을 찾을 수 없습니다","errors":null}"""

        private const val CURATION_INVALID_INPUT_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"잘못된 요청입니다","errors":[{"field":"position","message":"position은 필수입니다"}]}"""

        private const val POSITION_INVALID_INPUT_EXAMPLE = CURATION_INVALID_INPUT_EXAMPLE

        private const val CAREER_LEVEL_INVALID_INPUT_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"잘못된 요청입니다","errors":[{"field":"careerLevel","message":"careerLevel은 필수입니다"}]}"""

        private const val NOTIFICATION_SETTINGS_INVALID_INPUT_EXAMPLE =
            "{\"success\":false,\"data\":null,\"code\":\"COMMON-001\",\"message\":\"잘못된 요청입니다\"," +
                "\"errors\":[{\"field\":\"setCompletionReminderEnabled\",\"message\":\"setCompletionReminderEnabled는 필수입니다\"}]}"
    }
}
