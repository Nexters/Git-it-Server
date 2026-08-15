package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorKind
import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.ai.QuestionWriter
import com.nexters.gitit.infrastructure.repo.SourceBundler
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class QuestionGeneratorTest {
    @TempDir
    lateinit var repoRoot: Path

    @Test
    fun `개념마다 앵커 본문을 실어 학습 세트를 만든다`() {
        repoRoot.file("src/Router.kt", "class Router {\n    fun route() = Unit\n}")
        repoRoot.file("src/Store.kt", "class Store {\n    fun commit() = Unit\n}")

        // 콜 없이 흐름만 확인한다. 앵커 발췌가 실제로 전달되는지 함께 본다.
        // 콜이 동시에 나가므로 도착 순서가 아니라 개념 이름으로 찾는다.
        val seenBundles = ConcurrentHashMap<String, String>()
        val writer =
            QuestionWriter { concept, anchorBundle, depth ->
                seenBundles[concept.name] = anchorBundle
                LearningSetDraft(
                    orientation = "이 개념을 먼저 읽으세요.",
                    anchorSummaries = listOf(AnchorSummaryDraft(1, "정의된 자리")),
                    // 콜은 레벨 하나만 쓰고, 게이트의 하한(레벨당 4문제)은 그 레벨에 적용된다.
                    questions = List(4) { choiceQuestion(depth.name) },
                )
            }

        val sets =
            QuestionGenerator(SourceBundler(), writer, QuestionGate())
                .generate(repoRoot, listOf(anchored("라우팅", "src/Router.kt"), anchored("상태 관리", "src/Store.kt")))

        val routingBundle = seenBundles.getValue("라우팅")
        routingBundle shouldContain "[앵커 1] src/Router.kt:2-2 (DEFINITION)"
        routingBundle shouldContain "    2|     fun route() = Unit"
        sets.map { it.concept.name } shouldContainExactly listOf("라우팅", "상태 관리")
        val routing = sets.first()
        val cited = routing.questions.first().anchors
        cited.single().file shouldBe "src/Router.kt"
        routing.notes.single().summary shouldBe "정의된 자리"
    }

    @Test
    fun `콜이 던진 예외는 감싸지지 않고 그대로 올라온다`() {
        repoRoot.file("src/Router.kt", "class Router")

        val writer = QuestionWriter { _, _, _ -> throw BaseException(ErrorCode.QUESTION_GENERATION_FAILED) }

        // 동시 실행이 예외를 ExecutionException으로 감싸면 부르는 쪽이 판정을 사고로 읽는다.
        shouldThrow<BaseException> {
            QuestionGenerator(SourceBundler(), writer, QuestionGate())
                .generate(repoRoot, listOf(anchored("라우팅", "src/Router.kt")))
        }
    }

    private fun anchored(
        name: String,
        path: String,
    ) = AnchoredConcept(
        Concept(name, "근거", "README.md", listOf(path)),
        listOf(
            Anchor(path, 2, 2, AnchorKind.DEFINITION, "fun"),
            Anchor(path, 1, 1, AnchorKind.TRACE, "class"),
        ),
    )

    private fun choiceQuestion(depth: String) =
        QuestionDraft(
            depth = depth,
            type = "FLOW",
            format = "MULTIPLE_CHOICE",
            text = "질문",
            choices = listOf("A", "B", "C", "D"),
            answerIndex = 1,
            explanation = "해설",
            hints = listOf("힌트 1", "힌트 2"),
            rubric = RubricDraft(emptyList(), emptyList(), "", "", ""),
            sourceAnchors = listOf(1),
        )

    private fun Path.file(
        relative: String,
        text: String,
    ) = resolve(relative).also {
        it.parent.createDirectories()
        it.writeText(text)
    }
}
