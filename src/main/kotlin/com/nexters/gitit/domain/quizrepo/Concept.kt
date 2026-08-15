package com.nexters.gitit.domain.quizrepo

/**
 * 문서 분석이 확정한 개념. 앵커 단계가 읽는 유일한 입력입니다.
 *
 * [rationale]은 [sourceDoc] 본문에서 그대로 떼어온 구절이며, 요약이 아닙니다.
 * 원문이어야 문자열 대조로 진위를 검사할 수 있고, 문서를 다시 읽지 않고도
 * 뒷단계가 "왜 이렇게 설계했나"를 쓸 수 있습니다.
 *
 * [candidatePaths]는 이미 실재가 확인된 경로라, 받는 쪽은 존재 여부를 다시 의심하지 않아도 됩니다.
 *
 * 경로는 전부 레포 루트 기준 상대 경로입니다.
 * 절대 경로를 담으면 산출물이 특정 머신의 해제 위치에 묶여 재실행이 깨집니다.
 */
data class Concept(
    val name: String,
    val rationale: String,
    val sourceDoc: String,
    val candidatePaths: List<String>,
)
