package com.nexters.gitit.domain.auth

/**
 * 각 provider의 인증 API를 호출할 때 넘겨야 하는 입력값.
 *
 * provider마다 요구하는 파라미터도, 그걸 검증하는 방식도 다르기 때문에
 * 하나의 타입으로 합치지 않고 provider별로 나눠 둡니다.
 * 하위 타입을 추가하면 [OauthAuthenticator] 구현체의 분기가 컴파일 에러로 드러납니다.
 */
sealed class OauthCredential {
    data class Google(
        val idToken: String,
    ) : OauthCredential()

    data class Apple(
        val idToken: String,
    ) : OauthCredential()
}
