package com.nexters.gitit.domain.quizrepo

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode

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
) {
    /** 게이트를 지난 4지선다라면 반드시 있습니다. 없으면 저장되면 안 됐을 문제라, 오답으로 흘려보내지 않고 터집니다. */
    val requiredAnswerIndex: Int
        get() = answerIndex ?: error("4지선다인데 정답이 없습니다: questionId=$id")

    /** 게이트를 지난 서술형이라면 반드시 있습니다. */
    val requiredRubric: Rubric
        get() = rubric ?: error("서술형인데 채점 기준이 없습니다: questionId=$id")

    /**
     * 고른 선택지가 정답인지 판정합니다. 4지선다가 아니거나 [selectedIndex]가 선택지 범위 밖이면
     * [BaseException]을 던집니다 — 범위 밖 번호를 그냥 받으면 오답으로 굳어 재제출 전까지 남습니다.
     */
    fun grade(selectedIndex: Int): Boolean {
        requireFormat(QuestionFormat.MULTIPLE_CHOICE)
        if (selectedIndex !in choices.indices) {
            throw BaseException(ErrorCode.INVALID_INPUT, "선택지 범위를 벗어난 답변입니다")
        }
        return selectedIndex == answerIndex
    }

    /** 답의 형식은 푸는 쪽이 아니라 문제가 정합니다. [expected]가 이 문제의 형식이 아니면 [BaseException]을 던집니다. */
    fun requireFormat(expected: QuestionFormat) {
        if (format != expected) {
            throw BaseException(ErrorCode.INVALID_INPUT, "문제 형식과 맞지 않는 답변입니다")
        }
    }
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
