package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.repo.SourcePathIndex
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class ConceptGateTest {
    @TempDir
    lateinit var repoRoot: Path

    private val gate = ConceptGate()
    private val index = SourcePathIndex(listOf("src/Router.kt", "src/Store.kt"))

    @Test
    fun `원문 대조를 통과한 개념은 경로까지 확정된다`() {
        val readme =
            repoRoot.doc(
                "README.md",
                "라우팅은 `Router.kt`가 전담합니다.\n상태는 `Store.kt`에 모읍니다.",
            )

        val concepts =
            gate.confirm(
                repoRoot,
                listOf(readme),
                index,
                listOf(
                    ConceptCandidate("라우팅", "라우팅은 `Router.kt`가 전담합니다.", "README.md", listOf("Router.kt")),
                    ConceptCandidate("상태 관리", "상태는 `Store.kt`에 모읍니다.", "README.md", listOf("Store.kt")),
                ),
            )

        concepts shouldContainExactly
            listOf(
                Concept("라우팅", "라우팅은 `Router.kt`가 전담합니다.", "README.md", listOf("src/Router.kt")),
                Concept("상태 관리", "상태는 `Store.kt`에 모읍니다.", "README.md", listOf("src/Store.kt")),
            )
    }

    private fun Path.doc(
        relative: String,
        text: String,
    ): String {
        resolve(relative).also {
            it.parent.createDirectories()
            it.writeText(text)
        }
        return relative
    }
}
