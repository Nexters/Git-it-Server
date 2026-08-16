package com.nexters.gitit.infrastructure.quiz

/**
 * 개념 추출 콜이 돌려준 그대로의 후보. 아직 아무것도 검증되지 않았습니다.
 *
 * 게이트를 통과하기 전까지만 사는 타입이라 저장되지 않고, 파이프라인 밖으로도 나가지 않습니다.
 *
 * [pathHints]는 "관련 파일을 찾아낸 결과"가 아니라 "문서에 적혀 있던 경로·파일명을 옮겨 적은 것"입니다.
 * 실제 경로 확정은 코드가 인덱스로 하므로 지어낼 여지가 없습니다.
 */
data class ConceptCandidate(
    val name: String,
    val rationale: String,
    val sourceDoc: String,
    val pathHints: List<String>,
)
