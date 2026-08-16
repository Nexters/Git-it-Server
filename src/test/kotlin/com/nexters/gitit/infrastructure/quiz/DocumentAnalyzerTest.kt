package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.infrastructure.ai.ConceptExtractor
import com.nexters.gitit.infrastructure.repo.DocumentBundler
import com.nexters.gitit.infrastructure.repo.DocumentRanker
import com.nexters.gitit.infrastructure.repo.DocumentScanner
import com.nexters.gitit.infrastructure.repo.SourceTreeBundler
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class DocumentAnalyzerTest {
    @TempDir
    lateinit var repoRoot: Path

    @Test
    fun `문서에서 개념을 뽑아 경로까지 확정한다`() {
        repoRoot.file("README.md", "라우팅은 `Router.kt`가 전담합니다.\n상태는 `Store.kt`에 모읍니다.")
        repoRoot.file("src/Router.kt", "class Router")
        repoRoot.file("src/Store.kt", "class Store")

        // 콜 없이 흐름만 확인한다. 문서 번들과 소스 목록이 실제로 전달되는지 함께 본다.
        var seenBundle = ""
        var seenTree = ""
        val extractor =
            ConceptExtractor { bundle, sourceTree ->
                seenBundle = bundle
                seenTree = sourceTree
                listOf(
                    ConceptCandidate("라우팅", "라우팅은 `Router.kt`가 전담합니다.", "README.md", listOf("Router.kt")),
                    ConceptCandidate("상태 관리", "상태는 `Store.kt`에 모읍니다.", "README.md", listOf("Store.kt")),
                )
            }

        val analyzer =
            DocumentAnalyzer(
                DocumentScanner(),
                DocumentRanker(),
                DocumentBundler(),
                SourceTreeBundler(),
                extractor,
                ConceptGate(),
            )

        val concepts = analyzer.analyze(repoRoot)

        seenBundle shouldContain "--- README.md ---"
        seenTree shouldContain "src/Router.kt"
        concepts.map { it.name } shouldContainExactly listOf("라우팅", "상태 관리")
        concepts.flatMap { it.candidatePaths } shouldContainExactly listOf("src/Router.kt", "src/Store.kt")
    }

    private fun Path.file(
        relative: String,
        text: String,
    ) = resolve(relative).also {
        it.parent.createDirectories()
        it.writeText(text)
    }
}
