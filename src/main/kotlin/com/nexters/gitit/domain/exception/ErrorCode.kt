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

    // MEMBER
    MEMBER_NOT_FOUND("MEMBER-001", "회원을 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // REPO
    // REPO_FETCH_FAILED만 재시도 대상이다. 나머지는 다시 시도해도 같은 결과라 즉시 거절한다.
    INVALID_REPO_URL("REPO-001", "레포 주소가 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    REPO_NOT_ACCESSIBLE("REPO-002", "레포를 찾을 수 없거나 접근할 수 없습니다", HttpStatus.NOT_FOUND),
    REPO_FETCH_FAILED("REPO-003", "레포를 가져오지 못했습니다", HttpStatus.BAD_GATEWAY),
    INVALID_REPO_ARCHIVE("REPO-004", "레포 아카이브를 해제할 수 없습니다", HttpStatus.BAD_GATEWAY),

    // QUIZ
    // 재료가 없어서 거절하는 것이므로 재시도 대상이 아니다.
    NO_CONCEPTS("QUIZ-001", "문제를 만들 만한 개념을 찾지 못했습니다", HttpStatus.UNPROCESSABLE_CONTENT),
    CONCEPT_EXTRACTION_FAILED("QUIZ-002", "개념을 추출하지 못했습니다", HttpStatus.BAD_GATEWAY),
    ANCHOR_SELECTION_FAILED("QUIZ-003", "코드 근거를 찾지 못했습니다", HttpStatus.BAD_GATEWAY),
    QUESTION_GENERATION_FAILED("QUIZ-004", "문제를 생성하지 못했습니다", HttpStatus.BAD_GATEWAY),
}
