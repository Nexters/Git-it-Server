package com.nexters.gitit.infrastructure.quiz

/**
 * 생성 콜이 돌려준 그대로의 학습 세트. 아직 아무것도 검증되지 않았습니다.
 *
 * 게이트를 통과하기 전까지만 사는 타입이라 저장되지 않고, 파이프라인 밖으로도 나가지 않습니다.
 *
 * 스키마로 형식을 강제하면 모든 필드가 required가 되므로 nullable을 두지 않습니다.
 * 4지선다의 [QuestionDraft.rubric]은 빈 객체로, 서술형의 [QuestionDraft.choices]는 빈 배열로 옵니다.
 * 그 정리는 게이트가 도메인 타입으로 옮기면서 합니다.
 */
data class LearningSetDraft(
    val orientation: String,
    val anchorSummaries: List<AnchorSummaryDraft>,
    val questions: List<QuestionDraft>,
)

/** [anchor]는 프롬프트에 실린 앵커 번호입니다. */
data class AnchorSummaryDraft(
    val anchor: Int,
    val summary: String,
)

/**
 * [depth]·[type]·[format]을 enum이 아니라 문자열로 받는 이유는 [AnchorCandidate.kind]와 같습니다.
 * 라벨 하나가 어긋났다고 응답 전체의 파싱이 깨지면 개념이 통째로 죽습니다.
 *
 * [sourceAnchors]는 프롬프트에 실린 앵커 번호이고, [answerIndex]는 서술형에서 -1로 옵니다.
 */
data class QuestionDraft(
    val depth: String,
    val type: String,
    val format: String,
    val text: String,
    val choices: List<String>,
    val answerIndex: Int,
    val explanation: String,
    val hints: List<String>,
    val rubric: RubricDraft,
    val sourceAnchors: List<Int>,
)

data class RubricDraft(
    val criteria: List<RubricCriterionDraft>,
    val keyPoints: List<String>,
    val fullMarkExample: String,
    val partialExample: String,
    val zeroExample: String,
)

data class RubricCriterionDraft(
    val text: String,
    val points: Int,
)
