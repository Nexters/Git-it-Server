package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorKind
import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.QuestionFormat
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class QuestionGateTest {
    private val gate = QuestionGate()

    private val routerDefinition = Anchor("src/Router.kt", 10, 20, AnchorKind.DEFINITION, "fun route")
    private val routerUsage = Anchor("src/RouterTest.kt", 5, 9, AnchorKind.USAGE, "route(")

    @Test
    fun `앵커 번호가 실제 앵커로 확정되고 서술형은 루브릭을 갖는다`() {
        val sets =
            gate.confirm(
                listOf(
                    anchored("라우팅") to draft(levelled(listOf(1))),
                    anchored("상태 관리") to draft(levelled(listOf(2))),
                ),
            )

        sets.map { it.concept.name } shouldContainExactly listOf("라우팅", "상태 관리")

        val routing = sets.first()
        routing.notes.map { it.anchor } shouldContainExactly listOf(routerDefinition, routerUsage)
        routing.questions
            .getValue(Depth.L1)
            .first()
            .anchors shouldContainExactly listOf(routerDefinition)

        val essay = routing.questions.getValue(Depth.L3).last()
        essay.format shouldBe QuestionFormat.ESSAY
        essay.answerIndex shouldBe null
        essay.choices shouldContainExactly emptyList()
        val rubric = essay.rubric.shouldNotBeNull()
        rubric.criteria.first().points shouldBe 10
    }

    @Test
    fun `선택지는 섞이고 정답은 따라 옮겨진다`() {
        val sets =
            gate.confirm(
                listOf(
                    anchored("라우팅") to draft(levelled(listOf(1))),
                    anchored("상태 관리") to draft(levelled(listOf(1))),
                ),
            )

        // 정답이 어느 자리로 갔는지는 보지 않는다. 섞이고 나서도 answerIndex가 같은 선택지를 가리키면 된다.
        sets.flatMap { it.questions.values.flatten() }.filter { it.format == QuestionFormat.MULTIPLE_CHOICE }.forEach {
            it.choices[it.answerIndex.shouldNotBeNull()] shouldBe "정답"
        }
    }

    @Test
    fun `선택지가 넷이 아닌 문제는 폐기되고 개념도 함께 탈락한다`() {
        val thrown =
            shouldThrow<BaseException> {
                gate.confirm(
                    listOf(
                        anchored("라우팅") to draft(levelled(listOf(1), choices = listOf("정답", "B", "C"))),
                        // 입력에 없는 3번 앵커를 근거로 들었다.
                        anchored("상태 관리") to draft(levelled(listOf(3))),
                    ),
                )
            }

        thrown.errorCode.code shouldBe "QUIZ-001"
    }

    @Test
    fun `세 레벨은 가장 얇은 레벨에 맞춰 같은 분량으로 잘린다`() {
        val sets =
            gate.confirm(
                listOf(
                    // L1만 여덟이어도 가장 얇은 L3이 다섯이면 세 레벨 모두 다섯이다. 상한은 없다.
                    anchored("라우팅") to draft(choiceQuestions("L1", 8) + choiceQuestions("L2", 6) + choiceQuestions("L3", 5)),
                    anchored("상태 관리") to draft(choiceQuestions("L1", 4) + choiceQuestions("L2", 6) + choiceQuestions("L3", 9)),
                    // L1이 하한(4)을 밑돌아 개념째 폐기된다.
                    anchored("빌드") to draft(choiceQuestions("L1", 3) + choiceQuestions("L2", 6) + choiceQuestions("L3", 6)),
                ),
            )

        sets.map { it.concept.name } shouldContainExactly listOf("라우팅", "상태 관리")
        sets.forEach { set ->
            set.questions.keys.toList() shouldContainExactly Depth.entries.toList()
        }
        sets.map { it.questions.getValue(Depth.L1).size } shouldContainExactly listOf(5, 4)
        sets.forEach { set ->
            set.questions.values
                .map { it.size }
                .distinct()
                .size shouldBe 1
        }
    }

    @Test
    fun `같은 것을 두 번 물은 문제는 하나만 남는다`() {
        // 레벨마다 6문제를 쓰되 L1의 두 문제가 본문까지 같다. 중복이 걷힌 뒤 L1이 가장 얇은 레벨이 된다.
        val duplicated = choiceQuestions("L1", 5) + choiceQuestions("L1", 1)

        val sets =
            gate.confirm(
                listOf(
                    anchored("라우팅") to draft(duplicated + choiceQuestions("L2", 6) + choiceQuestions("L3", 6)),
                    anchored("상태 관리") to draft(choiceQuestions("L1", 6) + choiceQuestions("L2", 6) + choiceQuestions("L3", 6)),
                ),
            )

        sets
            .first()
            .questions
            .getValue(Depth.L1)
            .size shouldBe 5
        sets
            .last()
            .questions
            .getValue(Depth.L1)
            .size shouldBe 6
    }

    private fun anchored(name: String) =
        AnchoredConcept(
            Concept(name, "근거", "README.md", listOf("src/Router.kt")),
            listOf(routerDefinition, routerUsage),
        )

    private fun draft(questions: List<QuestionDraft>) =
        LearningSetDraft(
            title = "라우팅 흐름 따라가기",
            description = "요청이 어느 경로로 흘러가는지 확인하는 학습 세트입니다.",
            orientation = "이 개념은 라우팅을 다룹니다.",
            anchorSummaries = listOf(AnchorSummaryDraft(1, "경로를 정의한다"), AnchorSummaryDraft(2, "경로를 호출한다")),
            questions = questions,
        )

    /** 프롬프트가 요구하는 그대로의 세트 — 레벨마다 6문제, L3의 절반은 서술형. */
    private fun levelled(
        sourceAnchors: List<Int>,
        choices: List<String> = listOf("정답", "B", "C", "D"),
    ) = choiceQuestions("L1", 6, sourceAnchors, choices) +
        choiceQuestions("L2", 6, sourceAnchors, choices) +
        choiceQuestions("L3", 3, sourceAnchors, choices) +
        List(3) { essayQuestion(sourceAnchors, it) }

    private fun choiceQuestions(
        depth: String,
        count: Int,
        sourceAnchors: List<Int> = listOf(1),
        choices: List<String> = listOf("정답", "B", "C", "D"),
    ) = List(count) { choiceQuestion(depth, sourceAnchors, choices, it) }

    private fun choiceQuestion(
        depth: String,
        sourceAnchors: List<Int>,
        choices: List<String>,
        ordinal: Int,
    ) = question(depth, "MULTIPLE_CHOICE", sourceAnchors, choices, answerIndex = 0, rubric = emptyRubric(), ordinal = ordinal)

    private fun essayQuestion(
        sourceAnchors: List<Int>,
        ordinal: Int,
    ) = question(
        depth = "L3",
        format = "ESSAY",
        sourceAnchors = sourceAnchors,
        choices = emptyList(),
        answerIndex = -1,
        rubric = emptyRubric().copy(criteria = listOf(RubricCriterionDraft("실제 파일명을 들었는가", 10))),
        ordinal = ordinal,
    )

    private fun question(
        depth: String,
        format: String,
        sourceAnchors: List<Int>,
        choices: List<String>,
        answerIndex: Int,
        rubric: RubricDraft,
        ordinal: Int,
    ) = QuestionDraft(
        depth = depth,
        type = "FLOW",
        format = format,
        // 셔플 시드도 식별자도 본문에서 나오므로, 본문이 같으면 배치가 겹치고 중복으로 걷힌다.
        text = "질문 ${choices.size}${sourceAnchors.joinToString()}$depth$ordinal",
        choices = choices,
        answerIndex = answerIndex,
        explanation = "해설",
        hints = listOf("힌트 1", "힌트 2"),
        rubric = rubric,
        sourceAnchors = sourceAnchors,
    )

    private fun emptyRubric() = RubricDraft(emptyList(), emptyList(), "", "", "")
}
