package com.nexters.gitit.domain.exception

/**
 * ErrorCode 기반으로 비즈니스 예외를 던지기 위한 공통 예외.
 * customMessage를 생략하면 errorCode.message가 기본 메시지로 사용됩니다.
 *
 * 적절한 ErrorCode가 없다면, ErrorCode 양식에 맞춰 생성합니다.
 *
 * 클라이언트에 내려줄 정보만 담습니다. 원인 예외는 cause로 싣지 말고 던지는 자리에서 로그로 남깁니다.
 * GlobalExceptionHandler가 BaseException을 로깅하지 않아 cause를 실어도 아무데도 찍히지 않습니다.
 */
class BaseException(
    val errorCode: ErrorCode,
    customMessage: String? = null,
) : RuntimeException(customMessage ?: errorCode.message)
