package com.nexters.gitit.domain.quizrepo

/**
 * 개념 하나짜리 학습 세트. 학습자에게 실제로 보이는 단위입니다.
 *
 * 학습 이력이 [id]로 이 세트를 가리킵니다. [Concept.name]은 중복이 걸러지지 않아 키가 되지 못합니다.
 *
 * 산문 셋은 읽히는 자리가 다릅니다. [title]·[description]은 세트에 들어가기 전 목록에서,
 * [orientation]은 들어간 뒤 문제를 풀기 전에 읽힙니다. [orientation]에는 문제로 낼 가치가 없는
 * 사실(폴더 구조·진입점·사용 라이브러리)이 담깁니다.
 *
 * [questions]는 완성된 세트라면 [Depth]마다 같은 수만큼 들어 있습니다. 학습자가 세트가 아니라
 * 레벨 하나를 골라 풀기 때문입니다. 만드는 도중에는 세다 만 상태가 있어 생성자가 막지 않습니다.
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
 * 짝지어 두어야 요약을 그 앵커 근처 코드로만 대조할 수 있습니다. 레포 전체에서 찾으면
 * `User`·`save` 같은 흔한 이름이 어디에나 있어 무엇이든 통과합니다.
 */
data class AnchorNote(
    val anchor: Anchor,
    val summary: String,
)
