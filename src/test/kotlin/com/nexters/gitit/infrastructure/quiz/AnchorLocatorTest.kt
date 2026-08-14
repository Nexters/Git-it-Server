package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.quizrepo.AnchorKind
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.ai.AnchorSelector
import com.nexters.gitit.infrastructure.repo.SourceBundler
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class AnchorLocatorTest {
    @TempDir
    lateinit var repoRoot: Path

    @Test
    fun `개념마다 후보 파일을 읽혀 앵커를 확정한다`() {
        repoRoot.file("src/Router.kt", "class Router {\n    fun route() = Unit\n}")
        repoRoot.file("src/Store.kt", "class Store {\n    fun commit() = Unit\n}")

        // 콜 없이 흐름만 확인한다. 번들이 실제로 전달되는지 함께 본다.
        // 콜이 동시에 나가므로 도착 순서가 아니라 개념 이름으로 찾는다.
        val seenBundles = ConcurrentHashMap<String, String>()
        val selector =
            AnchorSelector { concept, bundle ->
                seenBundles[concept.name] = bundle
                val file = concept.candidatePaths.first()
                listOf(
                    AnchorCandidate(file, 1, 1, "DEFINITION", file.substringAfterLast('/').removeSuffix(".kt")),
                    AnchorCandidate(file, 2, 2, "usage", "fun"),
                )
            }

        val locator = AnchorLocator(SourceBundler(), selector, AnchorGate(SourceBundler()))

        val anchored =
            locator.locate(
                repoRoot,
                listOf(
                    Concept("라우팅", "근거", "README.md", listOf("src/Router.kt")),
                    Concept("상태 관리", "근거", "README.md", listOf("src/Store.kt")),
                ),
            )

        seenBundles.getValue("라우팅") shouldContain "--- src/Router.kt (총 3줄) ---"
        anchored.map { it.concept.name } shouldContainExactly listOf("라우팅", "상태 관리")
        anchored.flatMap { concept -> concept.anchors.map { it.kind } } shouldContainExactly
            listOf(AnchorKind.DEFINITION, AnchorKind.USAGE, AnchorKind.DEFINITION, AnchorKind.USAGE)
    }

    private fun Path.file(
        relative: String,
        text: String,
    ) = resolve(relative).also {
        it.parent.createDirectories()
        it.writeText(text)
    }
}
