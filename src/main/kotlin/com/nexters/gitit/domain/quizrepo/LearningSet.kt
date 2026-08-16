package com.nexters.gitit.domain.quizrepo

/**
 * 개념 하나짜리 학습 세트. 학습자에게 실제로 보이는 단위입니다.
 *
 * [Concept.name]이 아니라 [id]로 가리킵니다. 이름은 콜이 지어낸 문자열이고 중복이 걸러지지 않아,
 * 같은 이름이 둘이면 학습 이력이 어느 세트의 것인지 알 수 없습니다.
 *
 * 산문이 셋인 것은 읽히는 자리가 셋이라서입니다. [title]·[description]은 세트에 들어가기 전 목록에서,
 * [orientation]은 들어간 뒤 문제를 풀기 전에 읽힙니다. [orientation]은 문제로 낼 가치가 없는
 * 사실(폴더 구조·진입점·사용 라이브러리)을 흡수하는 자리이자 학습자가 가장 많이 읽는 글이라,
 * 뒷단계 검증이 문제보다 여기를 먼저 봅니다.
 *
 * [questions]는 [Depth]마다 같은 수만큼 들어 있습니다. 학습자가 세트가 아니라 세트의 레벨 하나를
 * 골라 풀기 때문입니다. 개수를 생성자에서 막지 않는 이유는 만드는 도중에는 세다 만 상태가
 * 존재하기 때문입니다 — 완성된 세트만 이 규칙을 지킵니다.
 */
data class LearningSet(
    val id: String,
    val concept: Concept,
    val title: String,
    val description: String,
    val orientation: String,
    val notes: List<AnchorNote>,
    val questions: Map<Depth, List<Question>>,
    val tags: Set<QualityTag> = emptySet(),
) {
    /** 그 레벨의 문제. 완성된 세트라면 세 레벨이 같은 수로 차 있고, 만들다 만 세트는 비어 있을 수 있습니다. */
    fun questionsOf(depth: Depth): List<Question> = questions[depth].orEmpty()

    /** 그 앵커를 풀어 쓴 요약. 짝지어 둔 [AnchorNote]가 없으면 null입니다. */
    fun summaryOf(anchor: Anchor): String? = notes.find { it.anchor == anchor }?.summary
}

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
