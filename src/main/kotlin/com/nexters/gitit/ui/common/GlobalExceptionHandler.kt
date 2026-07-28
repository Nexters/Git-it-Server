package com.nexters.gitit.ui.common

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {
    /**
     * ErrorCode에 정의된 status와 message(또는 customMessage)를 그대로 응답에 반영합니다.
     * 어떤 status/message를 내려줄지는 예외를 던진 쪽(ErrorCode)이 결정합니다.
     */
    @ExceptionHandler(BaseException::class)
    fun handleBaseException(e: BaseException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode, e.message))

    /**
     * `@Valid @RequestBody` 검증 실패 시 발생합니다.
     * BindingResult에 담긴 필드별 실패 사유를 그대로 errors로 내려줍니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors =
            e.bindingResult.fieldErrors.map { ApiResponse.FieldError(it.field, it.defaultMessage) }
        return ResponseEntity
            .status(ErrorCode.INVALID_INPUT.status)
            .body(ApiResponse.error(ErrorCode.INVALID_INPUT, errors))
    }

    /**
     * `@Validated` + `@RequestParam`/`@PathVariable`처럼 BindingResult를 거치지 않는
     * 파라미터 검증 실패를 처리합니다. MethodArgumentNotValidException과는 별개 경로라
     * 둘 다 등록해둬야 검증 실패가 새지 않습니다.
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
        val errors =
            e.constraintViolations.map { ApiResponse.FieldError(it.propertyPath.toString(), it.message) }
        return ResponseEntity
            .status(ErrorCode.INVALID_INPUT.status)
            .body(ApiResponse.error(ErrorCode.INVALID_INPUT, errors))
    }

    /**
     * 요청 본문을 JSON으로 파싱하는 단계에서 실패한 경우 발생합니다.
     * `@Valid` 검증이 실행되기 전이라 필드 단위 정보가 없어 단일 메시지로만 응답합니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug(e) { "Malformed request body" }
        return ResponseEntity
            .status(ErrorCode.INVALID_INPUT.status)
            .body(ApiResponse.error(ErrorCode.INVALID_INPUT, "요청 본문을 읽을 수 없습니다"))
    }

    /**
     * path/query 파라미터를 선언된 타입으로 변환하지 못한 경우 발생합니다.
     * 예: Long 파라미터에 "abc"가 전달된 경우.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(ErrorCode.INVALID_INPUT.status)
            .body(ApiResponse.error(ErrorCode.INVALID_INPUT, "${e.name}의 형식이 올바르지 않습니다"))

    /**
     * 필수로 선언된 쿼리 파라미터가 요청에 없는 경우 발생합니다.
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(ErrorCode.INVALID_INPUT.status)
            .body(ApiResponse.error(ErrorCode.INVALID_INPUT, "${e.parameterName}은(는) 필수 파라미터입니다"))

    /**
     * 위 핸들러들이 잡지 못한 모든 예외의 최종 방어선입니다.
     * 클라이언트에는 스택트레이스 없이 일반화된 메시지만 내려주고, 원인 추적을 위해
     * 서버 로그에는 예외 전체를 남깁니다.
     */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error(e) { "Unhandled exception" }
        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.status)
            .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR))
    }
}
