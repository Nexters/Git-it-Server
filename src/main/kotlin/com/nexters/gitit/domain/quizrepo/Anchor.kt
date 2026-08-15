package com.nexters.gitit.domain.quizrepo

/**
 * 개념이 가리키는 코드 위치. 파이프라인의 신뢰성 전체가 여기 걸려 있습니다.
 *
 * 문서는 "무엇을 물을 것인가"를 정하는 최고의 소스지만 거짓말을 합니다 — 코드보다 늦게 갱신되고,
 * 지향과 구현이 어긋납니다. 앵커는 그 주장이 지금도 참인지에 대한 근거이고,
 * 앵커가 붙지 않는 주장은 설계 결정이 아니라 홍보 문구로 봅니다.
 *
 * [symbol]은 그 라인 범위에 실제로 적혀 있던 식별자입니다. 앵커가 진짜인지 대조하는 유일한 수단이라
 * 검증이 끝난 뒤에도 버리지 않습니다 — 뒷단계의 산문 검증도 같은 문자열을 씁니다.
 *
 * [file]은 레포 루트 기준 상대 경로입니다. 절대 경로를 담으면 산출물이 특정 머신의 해제 위치에 묶입니다.
 */
data class Anchor(
    val file: String,
    val startLine: Int,
    val endLine: Int,
    val kind: AnchorKind,
    val symbol: String,
)

/**
 * 앵커가 코드의 어떤 면을 짚는지.
 *
 * [USAGE]는 테스트 파일을 우선합니다. 테스트는 CI에서 계속 도는 "검증된 동작의 문서"라
 * 문서와 달리 낡지 않고, assertion 자체가 기대 동작의 명세이기 때문입니다.
 */
enum class AnchorKind {
    DEFINITION,
    USAGE,
    TRACE,
}

/**
 * 앵커까지 확정된 개념. 문제 생성이 읽는 유일한 입력입니다.
 *
 * [Concept]에 앵커 필드를 얹지 않고 감싸는 이유는, 앞 단계 산출물의 모양을 그대로 두어야
 * 문서 분석과 앵커 단계를 따로 다시 돌릴 수 있기 때문입니다.
 *
 * 문서 근거([Concept.rationale])를 여기까지 들고 오는 이유는 문제 생성이 코드와 근거를 모두 봐야 하기 때문입니다.
 * 근거가 없으면 코드만 보고 의도를 지어내는데, 식별자는 맞아서 뒷단계 검증에도 걸리지 않습니다.
 */
data class AnchoredConcept(
    val concept: Concept,
    val anchors: List<Anchor>,
)
