package com.nexters.gitit.ui.common

import com.nexters.gitit.domain.exception.ErrorCode

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val code: String? = null,
    val message: String? = null,
    val errors: List<FieldError>? = null,
) {
    /**
     * 검증 실패한 필드 정보.
     * message는 Spring FieldError#defaultMessage를 그대로 전달하며, validator가
     * 메시지를 지정하지 않은 경우 null일 수 있습니다.
     */
    data class FieldError(
        val field: String,
        val message: String?,
    )

    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        /**
         * 내려줄 본문이 없는 성공 응답.
         * success(Unit)으로 대신하면 data가 빈 객체({})로 직렬화돼 없는 값과 구분되지 않습니다.
         */
        fun success(): ApiResponse<Unit> = ApiResponse(success = true)

        /**
         * 단일 메시지로 실패 응답을 만듭니다.
         * message를 생략하면 errorCode.message가 기본 메시지로 사용됩니다.
         */
        fun error(
            errorCode: ErrorCode,
            message: String? = null,
        ): ApiResponse<Nothing> = ApiResponse(success = false, code = errorCode.code, message = message ?: errorCode.message)

        /**
         * 필드별 검증 실패로 응답을 만듭니다.
         * message는 errorCode.message로 고정되고, 필드별 상세 사유는 errors로 전달합니다.
         */
        fun error(
            errorCode: ErrorCode,
            errors: List<FieldError>,
        ): ApiResponse<Nothing> = ApiResponse(success = false, code = errorCode.code, message = errorCode.message, errors = errors)
    }
}
