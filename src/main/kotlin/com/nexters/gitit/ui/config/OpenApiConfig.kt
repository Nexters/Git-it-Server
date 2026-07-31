package com.nexters.gitit.ui.config

import com.nexters.gitit.domain.exception.ErrorCode
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.GlobalOperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Configuration
class OpenApiConfig {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("git-it API")
                .description("git-it 서비스 API 문서")
                .version("v1"),
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
                    ApiResponse()
                        .description(ErrorCode.INTERNAL_SERVER_ERROR.message)
                        .content(
                            Content().addMediaType(
                                APPLICATION_JSON_VALUE,
                                // 예시를 문자열이 아니라 Map으로 주어야 JSON 객체로 렌더링됩니다.
                                MediaType().example(errorExample(ErrorCode.INTERNAL_SERVER_ERROR)),
                            ),
                        )
                }
            }
        }

    private fun errorExample(errorCode: ErrorCode) =
        mapOf(
            "success" to false,
            "data" to null,
            "code" to errorCode.code,
            "message" to errorCode.message,
            "errors" to null,
        )
}
