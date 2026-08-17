package com.nexters.gitit.ui.auth

import com.nexters.gitit.ui.auth.dto.AppleLoginRequest
import com.nexters.gitit.ui.auth.dto.GoogleLoginRequest
import com.nexters.gitit.ui.auth.dto.LoginResponse
import com.nexters.gitit.ui.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Auth", description = "로그인 API")
interface AuthControllerDocs {
    @Operation(
        summary = "구글 로그인",
        description = "구글 ID 토큰을 검증해 로그인합니다. 가입 이력이 없으면 이 요청이 곧 회원가입이 됩니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "로그인 성공",
        ),
        SwaggerApiResponse(
            responseCode = "400",
            description = "idToken이 비어 있음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = INVALID_INPUT_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "401",
            description = "ID 토큰 검증 실패",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = UNAUTHORIZED_EXAMPLE)])],
        ),
    )
    fun googleLogin(request: GoogleLoginRequest): ApiResponse<LoginResponse>

    @Operation(
        summary = "애플 로그인",
        description =
            "애플 ID 토큰을 검증해 로그인합니다. 가입 이력이 없으면 이 요청이 곧 회원가입이 됩니다. " +
                "애플은 최초 인가 때만 이메일을 내려주므로 재로그인 시에는 이메일이 갱신되지 않습니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "로그인 성공",
        ),
        SwaggerApiResponse(
            responseCode = "400",
            description = "idToken이 비어 있음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = INVALID_INPUT_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "401",
            description = "ID 토큰 검증 실패",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = UNAUTHORIZED_EXAMPLE)])],
        ),
    )
    fun appleLogin(request: AppleLoginRequest): ApiResponse<LoginResponse>

    @Operation(
        summary = "액세스 토큰 유효성 확인",
        description =
            "가지고 있는 액세스 토큰이 아직 쓸 수 있는지 확인합니다. 유효하면 200, 만료됐으면 401입니다. " +
                "만료와 서명 위조를 응답에서 구분하지 않으므로, 401을 받으면 다시 로그인해야 합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "토큰이 유효함",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = TOKEN_VALID_EXAMPLE)])],
        ),
    )
    fun verifyAccessToken(memberId: String): ApiResponse<Unit>

    companion object {
        // 성공 응답은 반환 타입에서 스키마가 추론되지만, 에러 응답은 제네릭이 ApiResponse<Nothing>이라
        // 스키마로 의미가 드러나지 않습니다. 그래서 에러만 예시로 보여줍니다.
        private const val INVALID_INPUT_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"잘못된 요청입니다","errors":[{"field":"idToken","message":"idToken은 필수입니다"}]}"""

        // 만료·서명 불일치·issuer 불일치를 구분하지 않습니다. 어떤 검증이 틀렸는지 알려주면 토큰 위조에 힌트가 되기 때문입니다.
        private const val UNAUTHORIZED_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-002","message":"ID 토큰 검증에 실패했습니다","errors":null}"""

        // 토큰 확인은 만료 여부만 알려주면 되므로 본문이 없습니다.
        // 만료됐을 때의 401은 OpenApiConfig의 loginMemberSecurityCustomizer가 @LoginMember를 보고 자동으로 붙입니다.
        private const val TOKEN_VALID_EXAMPLE =
            """{"success":true,"data":null,"code":null,"message":null,"errors":null}"""
    }
}
