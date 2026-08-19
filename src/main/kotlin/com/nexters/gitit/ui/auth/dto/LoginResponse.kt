package com.nexters.gitit.ui.auth.dto

import com.nexters.gitit.application.auth.Login
import io.swagger.v3.oas.annotations.media.Schema

/**
 * memberId는 내려주지 않습니다. accessToken의 subject에 이미 담겨 있어 중복이고,
 * 클라이언트가 별도로 필요로 하는 시점에 추가하는 편이 노출 범위를 좁게 유지합니다.
 */
data class LoginResponse(
    @field:Schema(description = "이후 API 호출의 Authorization 헤더에 담아 보낼 토큰")
    val accessToken: String,
    @field:Schema(description = "accessToken이 만료됐을 때 재발급에 사용할 토큰")
    val refreshToken: String,
    @field:Schema(description = "true면 아직 큐레이션 정보를 받지 않은 회원이므로 온보딩 화면으로 보내야 합니다")
    val needsCuration: Boolean,
) {
    companion object {
        fun from(result: Login.Result) =
            LoginResponse(
                accessToken = result.jwtToken.accessToken,
                refreshToken = result.jwtToken.refreshToken,
                needsCuration = result.needsCuration,
            )
    }
}
