package com.nexters.gitit.domain.quizrepo

/**
 * 앵커에 묶인 문제 하나.
 *
 * [id]는 회원별 학습 이력(정답 여부·저장한 문제)이 문제를 가리키는 키입니다. 임베드 도큐먼트라 Mongo가
 * `_id`를 붙여주지 않아 만드는 쪽이 채웁니다. **기본값을 두면 안 됩니다** — id 없는 옛 도큐먼트를 읽을
 * 때마다 새 id가 조용히 생겨 거기 매달린 이력이 미아가 됩니다. 읽는 즉시 터지는 편이 낫습니다.
 *
 * [depth]는 [LearningSet.questions]가 레벨로 중첩된 뒤에도 남습니다. 저장해 둔 문제를 목록 밖에서
 * 단독으로 보여줄 때 레벨을 알 방법이 이 필드뿐입니다.
 *
 * [anchors]는 번호가 아니라 확정된 위치입니다. 콜은 프롬프트에 실린 번호로 답하지만,
 * 그 번호를 실제 앵커로 바꾸는 일은 코드가 합니다 — 받는 쪽이 다시 매핑하지 않아도 됩니다.
 *
 * [choices]와 [answerIndex]는 4지선다에서만, [rubric]은 서술형에서만 채워집니다.
 */
data class Question(
    val id: String,
    val depth: Depth,
    val type: QuestionType,
    val format: QuestionFormat,
    val text: String,
    val choices: List<String>,
    val answerIndex: Int?,
    val explanation: String,
    val hints: List<String>,
    val rubric: Rubric?,
    val anchors: List<Anchor>,
    val tags: Set<QualityTag> = emptySet(),
)

/**
 * 문제가 요구하는 이해의 깊이. 직급(주니어·시니어)에 매핑하지 않습니다 —
 * 레포를 공부한다는 것 자체가 수준을 내포하고, "미들인데 주니어 문제를 틀렸다"는 경험은 학습을 방해합니다.
 *
 * [L1]은 앵커에 적힌 사실을 확인하면 닫히고, [L2]는 앵커 하나를 따라가며 그 코드가 무엇을 하는 자리인지 읽어야 하고,
 * [L3]은 앵커를 연결하거나 명시되지 않은 판단을 추론해야 합니다.
 *
 * 세 레벨을 모두 출제하는 이유는 세트가 레벨 단위로 서빙되기 때문입니다 —
 * L1이 비면 레포를 처음 여는 학습자에게 내줄 것이 없습니다.
 */
enum class Depth {
    L1,
    L2,
    L3,
}

/**
 * 서빙 필터 전용 라벨. 생성 quota에도 검증에도 쓰지 않습니다 —
 * 라벨이 틀렸을 때의 비용이 라벨을 검증하는 비용보다 싸기 때문입니다.
 */
enum class QuestionType {
    STRUCTURE,
    FLOW,
    CONCEPT,
    INTENT,
    IMPACT,
}

enum class QuestionFormat {
    MULTIPLE_CHOICE,
    ESSAY,
}

/**
 * 서술형 채점 기준. 채점자가 학습자 자신이라 필수입니다.
 *
 * [fullMarkExample]·[partialExample]·[zeroExample]이 핵심입니다 —
 * 기준표만 주면 자가채점이 후해지는데, "이런 답이면 몇 점"의 구체적인 예가 채점 분산을 줄입니다.
 */
data class Rubric(
    val criteria: List<RubricCriterion>,
    val keyPoints: List<String>,
    val fullMarkExample: String,
    val partialExample: String,
    val zeroExample: String,
)

/** 정답 나열이 아니라 판단 기준입니다 (예: "실제 파일명을 들어 설명했는가"). */
data class RubricCriterion(
    val text: String,
    val points: Int,
)
