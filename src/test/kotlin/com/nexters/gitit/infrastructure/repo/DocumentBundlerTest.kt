package com.nexters.gitit.infrastructure.repo

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class DocumentBundlerTest {
    @TempDir
    lateinit var repoRoot: Path

    private val bundler = DocumentBundler()

    @Test
    fun `예산 안이면 경로 머리말과 본문을 그대로 담는다`() {
        val readme = repoRoot.doc("README.md", "# 소개\n본문입니다.")
        val design = repoRoot.doc("docs/design.md", "# 설계\n결정 사항.")

        val bundle = bundler.bundle(repoRoot, listOf(readme, design))

        bundle shouldStartWith "--- README.md ---\n# 소개\n본문입니다."
        bundle shouldContain "--- docs/design.md ---\n# 설계\n결정 사항."
    }

    @Test
    fun `전문이 안 들어가는 문서는 헤딩만 담고 그렇다고 표시한다`() {
        val big = repoRoot.doc("big.md", "# 큰 문서\n" + "가".repeat(30_000) + "\n## 하위 제목\n버려질 본문")

        val bundle = bundler.bundle(repoRoot, listOf(big))

        bundle shouldStartWith "--- big.md [headings only] ---\n# 큰 문서\n## 하위 제목"
        bundle shouldNotContain "버려질 본문"
    }

    @Test
    fun `헤딩만 담을 때 코드 블록 안의 샵은 제목으로 보지 않는다`() {
        val big =
            repoRoot.doc(
                "big.md",
                """
                |# 시작하기
                |${"가".repeat(30_000)}
                |```bash
                |# install deps
                |npm install
                |```
                |## 배포
                """.trimMargin(),
            )

        val bundle = bundler.bundle(repoRoot, listOf(big))

        bundle shouldContain "# 시작하기\n## 배포"
        bundle shouldNotContain "install deps"
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
