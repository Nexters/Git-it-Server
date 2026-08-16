package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorKind
import com.nexters.gitit.domain.quizrepo.AnchorNote
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.LearningSet
import com.nexters.gitit.domain.quizrepo.QualityTag
import com.nexters.gitit.domain.quizrepo.Question
import com.nexters.gitit.domain.quizrepo.QuestionFormat
import com.nexters.gitit.domain.quizrepo.QuestionType
import com.nexters.gitit.infrastructure.repo.SourceBundler
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class QualityInspectorTest {
    @TempDir
    lateinit var repoRoot: Path

    private val inspector = QualityInspector(SourceBundler())

    private val anchor = Anchor("src/Router.kt", 1, 3, AnchorKind.DEFINITION, "Router")

    @Test
    fun `대조 범위에 없는 이름을 말하면 산문과 선택지에 표시가 붙는다`() {
        repoRoot.file("src/Router.kt", "class Router {\n    fun route() = Unit\n}")
        repoRoot.file("src/Store.kt", "class Store")

        val inspected =
            inspector.inspect(
                repoRoot,
                listOf(
                    set(
                        orientation = "요청은 `RouterRegistry`가 받습니다.",
                        summary = "`Router`가 경로를 정의합니다.",
                        choices = listOf("`Router`", "`Store`", "`PhantomHandler`", "`Router`의 하위 클래스"),
                    ),
                ),
            )

        inspected.single().tags shouldContainExactly setOf(QualityTag.PROSE_SUSPECT)
        inspected
            .single()
            .questions
            .values
            .flatten()
            .single()
            .tags shouldContainExactly setOf(QualityTag.DISTRACTOR_SUSPECT)
    }

    @Test
    fun `실재하는 이름만 쓰면 표시가 없고 L3인데 앵커가 하나면 깊이가 의심된다`() {
        repoRoot.file("src/Router.kt", "class Router {\n    fun route() = Unit\n}")
        // 같은 디렉터리 파일도 대조 범위다. Store는 앵커 파일에 없지만 통과해야 한다.
        repoRoot.file("src/Store.kt", "class Store")

        val inspected =
            inspector.inspect(
                repoRoot,
                listOf(
                    set(
                        orientation = "`Router`가 경로를 맡습니다.",
                        summary = "`route`를 호출합니다.",
                        choices = listOf("`Router`", "`Store`", "`route`", "`Router` 초기화"),
                        depth = Depth.L3,
                    ),
                ),
            )

        inspected.single().tags shouldContainExactly emptySet()
        inspected
            .single()
            .questions
            .values
            .flatten()
            .single()
            .tags shouldContainExactlyInAnyOrder setOf(QualityTag.DEPTH_SUSPECT)
    }

    private fun set(
        orientation: String,
        summary: String,
        choices: List<String>,
        depth: Depth = Depth.L2,
    ) = LearningSet(
        id = "set-1",
        concept = Concept("라우팅", "근거", "README.md", listOf("src/Router.kt")),
        title = "라우팅 흐름 따라가기",
        description = "요청이 어느 경로로 흘러가는지 확인하는 학습 세트입니다.",
        orientation = orientation,
        notes = listOf(AnchorNote(anchor, summary)),
        questions = mapOf(depth to listOf(question(choices, depth))),
    )

    private fun question(
        choices: List<String>,
        depth: Depth,
    ) = Question(
        id = "question-1",
        depth = depth,
        type = QuestionType.FLOW,
        format = QuestionFormat.MULTIPLE_CHOICE,
        text = "질문",
        choices = choices,
        answerIndex = 0,
        explanation = "해설",
        hints = listOf("힌트 1", "힌트 2"),
        rubric = null,
        anchors = listOf(anchor),
    )

    private fun Path.file(
        relative: String,
        text: String,
    ) = resolve(relative).also {
        it.parent.createDirectories()
        it.writeText(text)
    }
}
