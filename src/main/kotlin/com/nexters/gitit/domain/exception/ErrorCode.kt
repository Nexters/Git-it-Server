package com.nexters.gitit.domain.exception

import org.springframework.http.HttpStatus

/**
 * 도메인별 에러 코드 enum이 구현해야 하는 규약.
 * API 에러 응답에서 code/message/status를 일관된 형식으로 노출하기 위해 존재합니다.
 *
 * [code]: {도메인}-{001 ~ 999}로 대문자로 표기 (예: USER-001)
 * [message]: {도메인}(을/를) {상황 설명}습니다 (예: 사용자를 찾을 수 없습니다)
 * [status]: 사용자에게 반환될 HttpStatus로 개발 편의를 위해 spring에 의존
 */
enum class ErrorCode(
    val code: String,
    val message: String,
    val status: HttpStatus,
) {
    // COMMON
    INVALID_INPUT("COMMON-001", "잘못된 요청입니다", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("COMMON-002", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON-003", "권한이 없습니다", HttpStatus.FORBIDDEN),
    NOT_FOUND("COMMON-004", "요청한 리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("COMMON-005", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
}
