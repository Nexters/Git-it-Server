package com.nexters.gitit.ui.common

import com.nexters.gitit.application.auth.VerifyMember
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * [LoginMember]를 붙인 파라미터에 인증된 회원의 memberId를 주입합니다.
 * 토큰 자체의 검증은 [VerifyMember]가 하고, 여기서는 헤더에서 토큰을 꺼내는 일만 합니다.
 */
@Component
class LoginMemberArgumentResolver(
    private val verifyMember: VerifyMember,
) : HandlerMethodArgumentResolver {
    /**
     * String이 아닌 파라미터는 지원하지 않습니다.
     * 여기서 걸러내야 잘못된 타입에 애너테이션을 붙였을 때 애매하게 통과하지 않고 호출 시점에 드러납니다.
     */
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(LoginMember::class.java) && parameter.parameterType == String::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): String = verifyMember(accessTokenOf(webRequest))

    private fun accessTokenOf(webRequest: NativeWebRequest): String {
        val header = webRequest.getHeader(HttpHeaders.AUTHORIZATION).orEmpty()
        // Authorization 헤더는 Basic 같은 다른 스킴도 쓰므로, 접두사를 요구해야 raw 토큰과 구분된다.
        // RFC 7235에서 인증 스킴은 대소문자를 구분하지 않는다.
        val token =
            if (header.startsWith(BEARER_PREFIX, ignoreCase = true)) {
                header.substring(BEARER_PREFIX.length).trim()
            } else {
                ""
            }

        return token.ifBlank { throw BaseException(ErrorCode.UNAUTHORIZED, "인증 토큰이 필요합니다") }
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
