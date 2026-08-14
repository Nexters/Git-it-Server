package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorKind
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.repo.SourceBundler
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class AnchorGateTest {
    @TempDir
    lateinit var repoRoot: Path

    private val gate = AnchorGate(SourceBundler())

    @Test
    fun `줄 번호가 어긋나도 심볼이 근처에 있으면 앵커가 확정된다`() {
        val routing = concept("라우팅", "src/Router.kt")
        val state = concept("상태 관리", "src/Store.kt")
        repoRoot.file("src/Router.kt", source("fun route", "fun dispatch"))
        repoRoot.file("src/Store.kt", source("class Store", "fun commit"))

        val confirmed =
            gate.confirm(
                repoRoot,
                listOf(
                    // 콜이 준 4번은 실제 심볼이 있는 3번에서 한 줄 어긋난 값이다.
                    routing to
                        listOf(
                            candidate("src/Router.kt", 4, "fun route"),
                            candidate("src/Router.kt", 6, "fun dispatch", kind = "USAGE"),
                        ),
                    state to listOf(candidate("src/Store.kt", 3, "class Store"), candidate("src/Store.kt", 6, "fun commit")),
                ),
            )

        confirmed.map { it.concept.name } shouldContainExactly listOf("라우팅", "상태 관리")
        confirmed.first().anchors shouldContainExactly
            listOf(
                Anchor("src/Router.kt", 4, 4, AnchorKind.DEFINITION, "fun route"),
                Anchor("src/Router.kt", 6, 6, AnchorKind.USAGE, "fun dispatch"),
            )
    }

    @Test
    fun `심볼이 없는 앵커는 폐기되고 개념도 함께 탈락한다`() {
        val routing = concept("라우팅", "src/Router.kt")
        val state = concept("상태 관리", "src/Store.kt")
        repoRoot.file("src/Router.kt", source("fun route", "fun dispatch"))
        repoRoot.file("src/Store.kt", source("class Store", "fun commit"))

        val thrown =
            shouldThrow<BaseException> {
                gate.confirm(
                    repoRoot,
                    listOf(
                        routing to listOf(candidate("src/Router.kt", 3, "fun route"), candidate("src/Router.kt", 6, "fun navigate")),
                        state to listOf(candidate("src/Store.kt", 3, "class Store")),
                    ),
                )
            }

        thrown.errorCode.code shouldBe "QUIZ-001"
    }

    private fun concept(
        name: String,
        path: String,
    ) = Concept(name, "근거", "README.md", listOf(path))

    private fun candidate(
        file: String,
        line: Int,
        symbol: String,
        kind: String = "DEFINITION",
    ) = AnchorCandidate(file, line, line, kind, symbol)

    // 3번 줄과 6번 줄에만 심볼이 있는 파일. 나머지는 빈 줄이다.
    private fun source(
        third: String,
        sixth: String,
    ) = listOf("", "", third, "", "", sixth, "", "").joinToString("\n")

    private fun Path.file(
        relative: String,
        text: String,
    ) = resolve(relative).also {
        it.parent.createDirectories()
        it.writeText(text)
    }
}
