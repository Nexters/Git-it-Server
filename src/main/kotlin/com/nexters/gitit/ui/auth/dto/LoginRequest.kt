package com.nexters.gitit.ui.auth.dto

import com.nexters.gitit.application.Login
import com.nexters.gitit.domain.auth.OauthCredential
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * provider별로 요청 타입을 나눠 둡니다.
 *
 * 지금은 두 provider가 요구하는 값이 idToken 하나로 같지만, [OauthCredential]의 하위 타입과
 * 1:1로 대응시켜 두면 provider가 요구하는 필드가 갈라질 때 해당 타입만 고치면 됩니다.
 */
data class GoogleLoginRequest(
    @field:Schema(description = "구글에서 발급받은 ID 토큰", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2M...")
    @field:NotBlank(message = "idToken은 필수입니다")
    val idToken: String,
) {
    fun toCommand() = Login.Command(OauthCredential.Google(idToken))
}

data class AppleLoginRequest(
    @field:Schema(description = "애플에서 발급받은 ID 토큰", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ilc2V2N...")
    @field:NotBlank(message = "idToken은 필수입니다")
    val idToken: String,
) {
    fun toCommand() = Login.Command(OauthCredential.Apple(idToken))
}
