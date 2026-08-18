package com.nexters.gitit.infrastructure.repo

import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorKind
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class SourceBundlerTest {
    @TempDir
    lateinit var repoRoot: Path

    private val bundler = SourceBundler()

    @Test
    fun `긴 파일은 가운데를 버리고 실제 줄 번호를 유지한다`() {
        repoRoot.file("src/Big.kt", (1..400).joinToString("\n") { "line $it" })

        val bundle = bundler.bundle(repoRoot, listOf("src/Big.kt"))

        bundle shouldContain "--- src/Big.kt (총 400줄) ---"
        bundle shouldContain "  150| line 150"
        bundle shouldContain "... (100줄 생략) ..."
        // 잘린 뒤에도 번호를 다시 매기지 않아야 앵커 검증이 성립한다.
        bundle shouldContain "  251| line 251"
        bundle shouldNotContain "line 200"
    }

    @Test
    fun `후보가 많아도 다섯 파일까지만 싣는다`() {
        val paths = (1..7).map { "src/F$it.kt" }
        paths.forEach { repoRoot.file(it, "class ${it.substringAfterLast('/')}") }

        val bundle = bundler.bundle(repoRoot, paths)

        bundle shouldContain "--- src/F5.kt"
        bundle shouldNotContain "--- src/F6.kt"
    }

    @Test
    fun `코드 조각은 번호를 붙이고 그 범위만 실제 줄 번호로 발췌한다`() {
        repoRoot.file("src/Router.kt", (1..20).joinToString("\n") { "line $it" })

        val excerpt =
            bundler.excerpt(repoRoot, listOf(Anchor("src/Router.kt", 5, 7, AnchorKind.DEFINITION, "line 5")))

        excerpt shouldContain "[코드 1] src/Router.kt:5-7 (DEFINITION)"
        excerpt shouldContain "    5| line 5"
        excerpt shouldContain "    7| line 7"
        excerpt shouldNotContain "line 8"
    }

    private fun Path.file(
        relative: String,
        text: String,
    ) = resolve(relative).also {
        it.parent.createDirectories()
        it.writeText(text)
    }
}
