package com.nexters.gitit.domain.quizrepo

/**
 * 개념 하나짜리 학습 세트. 학습자에게 실제로 보이는 단위입니다.
 *
 * [questions]는 [Depth]마다 같은 수만큼 들어 있습니다. 학습자가 세트가 아니라 세트의 레벨 하나를
 * 골라 풀기 때문입니다. 개수를 생성자에서 막지 않는 이유는 만드는 도중에는 세다 만 상태가
 * 존재하기 때문입니다 — 완성된 세트만 이 규칙을 지킵니다.
 *
 * [orientation]은 문제로 낼 가치가 없는 사실(폴더 구조·진입점·사용 라이브러리)을 흡수하는 자리입니다.
 * 학습자가 가장 먼저·가장 많이 읽는 산문이기도 해서, 뒷단계 검증이 문제보다 여기를 먼저 봅니다.
 */
data class LearningSet(
    val concept: Concept,
    val orientation: String,
    val notes: List<AnchorNote>,
    val questions: List<Question>,
    val tags: Set<QualityTag> = emptySet(),
)

/**
 * 앵커 하나와 그 자리를 풀어 쓴 요약.
 *
 * 요약을 앵커와 짝지어 두는 이유는 검증 때문입니다. 산문에 등장한 식별자를 레포 전체에서 찾으면
 * `User`·`save` 같은 흔한 이름이 어디에나 있어 통과율이 100%가 됩니다.
 * 짝이 있어야 "이 요약은 이 파일 근처에서만 참"이라는 좁은 대조가 가능합니다.
 */
data class AnchorNote(
    val anchor: Anchor,
    val summary: String,
)
