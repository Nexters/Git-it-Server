package com.nexters.gitit.ui.config

import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.ui.common.LoginMember
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import org.springdoc.core.customizers.GlobalOperationCustomizer
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

/**
 * Authorize 버튼만 만듭니다. 인증을 요구할 API는 [loginMemberSecurityCustomizer]가 고릅니다.
 * 전역 SecurityRequirement로 걸면 모든 API에 자물쇠가 붙어 [LoginMember] 여부를 구분할 수 없습니다.
 */
@SecurityScheme(
    name = OpenApiConfig.BEARER_AUTH,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
)
@Configuration
class OpenApiConfig {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI().info(
            Info().title("git-it API").description("git-it 서비스 API 문서").version("v1"),
        )

    /**
     * 모든 엔드포인트에서 발생할 수 있는 500 응답을 자동으로 붙입니다.
     * 엔드포인트마다 같은 내용을 반복해 적지 않기 위한 것이라, 이미 500을 직접 선언한 오퍼레이션은 건드리지 않습니다.
     */
    @Bean
    fun internalServerErrorCustomizer() =
        GlobalOperationCustomizer { operation, _ ->
            val statusCode = ErrorCode.INTERNAL_SERVER_ERROR.status.value()

            operation.apply {
                responses.computeIfAbsent("$statusCode") {
                    ApiResponse().description(ErrorCode.INTERNAL_SERVER_ERROR.message).content(
                        Content().addMediaType(
                            APPLICATION_JSON_VALUE,
                            // 예시를 문자열이 아니라 Map으로 주어야 JSON 객체로 렌더링됩니다.
                            MediaType().example(errorExample(ErrorCode.INTERNAL_SERVER_ERROR)),
                        ),
                    )
                }
            }
        }

    /** [LoginMember] 파라미터가 있는 오퍼레이션에만 인증 요구와 401을 붙입니다. 시그니처에 이미 있는 사실을 문서에 또 적지 않기 위함입니다. */
    @Bean
    fun loginMemberSecurityCustomizer() =
        GlobalOperationCustomizer { operation, handlerMethod ->
            val requiresAuthentication = handlerMethod.methodParameters.any { it.hasParameterAnnotation(LoginMember::class.java) }

            if (requiresAuthentication) {
                operation.addSecurityItem(SecurityRequirement().addList(BEARER_AUTH))
                operation.responses.computeIfAbsent("${ErrorCode.UNAUTHORIZED.status.value()}") {
                    ApiResponse().description(ErrorCode.UNAUTHORIZED.message).content(
                        Content().addMediaType(
                            APPLICATION_JSON_VALUE,
                            MediaType().example(errorExample(ErrorCode.UNAUTHORIZED)),
                        ),
                    )
                }
            }

            operation
        }

    private fun errorExample(errorCode: ErrorCode) =
        mapOf(
            "success" to false,
            "data" to null,
            "code" to errorCode.code,
            "message" to errorCode.message,
            "errors" to null,
        )

    companion object {
        const val BEARER_AUTH = "bearerAuth"

        init {
            // springdoc은 애너테이션 없는 String을 쿼리 파라미터로 문서화한다. @LoginMember는 헤더에서 오므로 숨긴다.
            // static 초기화여야 springdoc 스캔보다 먼저 실행된다.
            SpringDocUtils.getConfig().addAnnotationsToIgnore(LoginMember::class.java)
        }
    }
}
