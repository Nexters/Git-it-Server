package com.nexters.gitit.ui.auth

import com.nexters.gitit.application.auth.Login
import com.nexters.gitit.ui.auth.dto.AppleLoginRequest
import com.nexters.gitit.ui.auth.dto.GoogleLoginRequest
import com.nexters.gitit.ui.auth.dto.LoginResponse
import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.common.LoginMember
import jakarta.validation.Valid
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth", produces = [APPLICATION_JSON_VALUE])
class AuthController(
    private val login: Login,
) : AuthControllerDocs {
    // 소셜 로그인은 첫 로그인이 곧 가입이지만 클라이언트에게는 언제나 로그인이므로 201이 아닌 200으로 응답합니다.
    @PostMapping("/login/google")
    override fun googleLogin(
        @Valid @RequestBody request: GoogleLoginRequest,
    ): ApiResponse<LoginResponse> = ApiResponse.success(LoginResponse.from(login(request.toCommand())))

    @PostMapping("/login/apple")
    override fun appleLogin(
        @Valid @RequestBody request: AppleLoginRequest,
    ): ApiResponse<LoginResponse> = ApiResponse.success(LoginResponse.from(login(request.toCommand())))

    // 검증은 [LoginMember]를 푸는 과정에서 끝나므로 본문이 비어 있습니다. 여기에 회원 조회를 더하면 DB를 타지 않는다는 이 엔드포인트의 전제가 깨집니다.
    @GetMapping("/token")
    override fun verifyAccessToken(
        @LoginMember memberId: String,
    ): ApiResponse<Unit> = ApiResponse.success()
}
