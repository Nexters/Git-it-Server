package com.nexters.gitit.domain.quizrepo

/**
 * 개념이 가리키는 코드 위치.
 *
 * [file]은 레포 루트 기준 상대 경로입니다. 절대 경로를 담으면 산출물이 특정 머신의 해제 위치에 묶입니다.
 *
 * [symbol]은 그 라인 범위에 실제로 적혀 있던 식별자입니다. 앵커와 산문이 진짜인지 대조하는 수단이라
 * 검증이 끝난 뒤에도 지우면 안 됩니다.
 */
data class Anchor(
    val file: String,
    val startLine: Int,
    val endLine: Int,
    val kind: AnchorKind,
    val symbol: String,
)

/** 앵커가 코드의 어떤 면을 짚는지. [DEFINITION]은 선언, [USAGE]는 호출부, [TRACE]는 흐름이 지나가는 자리입니다. */
enum class AnchorKind {
    DEFINITION,
    USAGE,
    TRACE,
}

/**
 * 앵커까지 확정된 개념. 문제 생성이 읽는 유일한 입력입니다.
 *
 * 문서 근거([Concept.rationale])가 [concept] 안에 그대로 딸려 옵니다. 문제 생성이 코드만 보면
 * 의도를 지어내는데, 식별자는 맞아서 뒷단계 검증에도 걸리지 않습니다.
 */
data class AnchoredConcept(
    val concept: Concept,
    val anchors: List<Anchor>,
)
