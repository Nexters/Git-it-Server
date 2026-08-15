package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorNote
import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.LearningSet
import com.nexters.gitit.domain.quizrepo.Question
import com.nexters.gitit.domain.quizrepo.QuestionFormat
import com.nexters.gitit.domain.quizrepo.QuestionType
import com.nexters.gitit.domain.quizrepo.Rubric
import com.nexters.gitit.domain.quizrepo.RubricCriterion
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * 생성 초안의 형식을 검사하고 앵커 번호를 실제 앵커로 확정합니다.
 *
 * 여기서 보는 것은 내용이 아니라 형식입니다 — 선택지가 4개인지, 정답 위치가 있는지,
 * 가리키는 앵커가 실재하는지. 내용이 참인지는 뒷단계가 태그로 판정합니다.
 *
 * 형식이 깨진 문제만 폐기하는 이유는 서빙이 불가능하기 때문입니다.
 * 선택지가 3개인 4지선다는 태그를 달아 봐야 학습자에게 보여줄 수 없습니다.
 */
@Component
class QuestionGate {
    /**
     * 검증을 통과한 학습 세트만 돌려줍니다.
     *
     * 서빙 단위가 레벨이라, 한 레벨이라도 [MINIMUM_QUESTIONS_PER_DEPTH]를 밑돈 개념은 폐기합니다 —
     * 그 레벨을 고른 학습자에게 내줄 것이 없기 때문입니다.
     * 살아남은 개념이 둘 미만이면 억지로 만들지 않고 [ErrorCode.NO_CONCEPTS]로 거절합니다.
     */
    fun confirm(drafts: List<Pair<AnchoredConcept, LearningSetDraft>>): List<LearningSet> {
        val confirmed = drafts.mapNotNull { (anchored, draft) -> confirm(anchored, draft) }

        if (confirmed.size < MINIMUM_CONCEPTS) {
            throw BaseException(ErrorCode.NO_CONCEPTS, "문제까지 만들어진 개념이 ${confirmed.size}개입니다")
        }
        return confirmed
    }

    private fun confirm(
        anchored: AnchoredConcept,
        draft: LearningSetDraft,
    ): LearningSet? {
        val name = anchored.concept.name
        val byDepth = draft.questions.mapNotNull { confirm(name, it, anchored.anchors) }.groupBy { it.depth }

        // 가장 얇은 레벨에 맞춘다. 레벨을 골랐는데 분량이 다르면 그 자체가 난이도로 읽힌다.
        val size = Depth.entries.minOf { byDepth[it]?.size ?: 0 }
        if (size < MINIMUM_QUESTIONS_PER_DEPTH) {
            logger.debug { "Discarded concept '$name': thinnest depth has $size question(s)" }
            return null
        }

        val notes =
            draft.anchorSummaries.mapNotNull { summary ->
                anchored.anchors.getOrNull(summary.anchor - 1)?.let { AnchorNote(it, summary.summary) }
            }

        // 콜이 순서를 뒤섞어 답해도 저장되는 세트는 늘 L1부터다.
        val questions = Depth.entries.flatMap { byDepth.getValue(it).take(size) }

        return LearningSet(anchored.concept, draft.orientation, notes, questions)
    }

    private fun confirm(
        name: String,
        draft: QuestionDraft,
        anchors: List<Anchor>,
    ): Question? {
        // 입력에 없던 번호를 답했다면 근거를 지어낸 것이다.
        val cited = draft.sourceAnchors.map { anchors.getOrNull(it - 1) }
        if (cited.isEmpty() || cited.any { it == null }) {
            logger.debug { "Discarded question of '$name': cites unknown anchor ${draft.sourceAnchors}" }
            return null
        }

        val format = formatOf(draft)
        if (!isWellFormed(draft, format)) {
            logger.debug { "Discarded question of '$name': malformed $format" }
            return null
        }

        val shuffled = if (format == QuestionFormat.MULTIPLE_CHOICE) shuffle(draft) else null

        return Question(
            depth = enumOf(draft.depth, Depth.entries, Depth.L2),
            type = enumOf(draft.type, QuestionType.entries, QuestionType.CONCEPT),
            format = format,
            text = draft.text,
            choices = shuffled?.first.orEmpty(),
            answerIndex = shuffled?.second,
            explanation = draft.explanation,
            hints = draft.hints,
            rubric = if (format == QuestionFormat.ESSAY) rubricOf(draft.rubric) else null,
            anchors = cited.filterNotNull(),
        )
    }

    private fun isWellFormed(
        draft: QuestionDraft,
        format: QuestionFormat,
    ): Boolean =
        when (format) {
            // 선택지가 겹치면 정답이 둘이 되거나 소거법으로 풀린다.
            QuestionFormat.MULTIPLE_CHOICE -> {
                draft.choices.size == CHOICE_COUNT &&
                    draft.choices.distinct().size == CHOICE_COUNT &&
                    draft.answerIndex in 0 until CHOICE_COUNT
            }

            // 자가채점이라 기준이 없는 서술형은 낼 수 없다.
            QuestionFormat.ESSAY -> {
                draft.rubric.criteria.isNotEmpty()
            }
        }

    /**
     * 모델이 정답을 앞자리에 몰아 넣어 코드가 섞습니다 — 한 세트의 4지선다가 전부 1번인 경우가 실제로 나왔고,
     * 그러면 학습자는 두 번째 세트부터 읽지 않고 찍습니다. 프롬프트로 부탁하는 것보다 확실합니다.
     *
     * 시드를 문제 본문에서 뽑는 이유는 재현성입니다. 같은 레포를 두 번 돌려도 같은 배치가 나와야 합니다.
     */
    private fun shuffle(draft: QuestionDraft): Pair<List<String>, Int> {
        val answer = draft.choices[draft.answerIndex]
        val shuffled = draft.choices.shuffled(Random(draft.text.hashCode()))

        return shuffled to shuffled.indexOf(answer)
    }

    private fun rubricOf(draft: RubricDraft) =
        Rubric(
            criteria = draft.criteria.map { RubricCriterion(it.text, it.points) },
            keyPoints = draft.keyPoints,
            fullMarkExample = draft.fullMarkExample,
            partialExample = draft.partialExample,
            zeroExample = draft.zeroExample,
        )

    // 라벨을 못 알아봤다고 문제를 버리지는 않는다. 라벨은 서빙 분류일 뿐이고 내용은 이미 검증됐다.
    private fun <T : Enum<T>> enumOf(
        raw: String,
        entries: List<T>,
        fallback: T,
    ): T = entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: fallback

    // 형식 라벨만은 추측이 가능하다. 선택지를 채웠다면 4지선다로 낸 것이다.
    private fun formatOf(draft: QuestionDraft): QuestionFormat {
        val labelled = QuestionFormat.entries.firstOrNull { it.name.equals(draft.format.trim(), ignoreCase = true) }
        return labelled ?: if (draft.choices.isEmpty()) QuestionFormat.ESSAY else QuestionFormat.MULTIPLE_CHOICE
    }

    companion object {
        private const val MINIMUM_CONCEPTS = 2
        private const val CHOICE_COUNT = 4

        // 레벨 하나가 한자리에서 푸는 단위다. 하한만 둔다 — 상한을 두면 잘 나온 세트를 스스로 깎게 된다.
        private const val MINIMUM_QUESTIONS_PER_DEPTH = 4
    }
}
