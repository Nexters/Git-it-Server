package com.nexters.gitit.infrastructure.repo

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class DocumentRankerTest {
    @TempDir
    lateinit var repoRoot: Path

    private val ranker = DocumentRanker()
    private val index = SourcePathIndex(listOf("src/App.kt", "src/Router.kt", "src/Store.kt"))

    @Test
    fun `실존 파일을 많이 언급한 문서가 앞선다`() {
        val many = repoRoot.doc("docs/many.md", "`App.kt`와 `Router.kt`와 `Store.kt`를 봅니다.")
        val few = repoRoot.doc("docs/few.md", "`App.kt`만 봅니다.")
        val none = repoRoot.doc("docs/none.md", "`Missing.kt`는 없는 파일입니다.")

        val ranked = ranker.rank(repoRoot, listOf(none, few, many), index)

        ranked shouldContainExactly listOf("docs/many.md", "docs/few.md", "docs/none.md")
    }

    @Test
    fun `README는 언급이 하나도 없어도 맨 앞이다`() {
        val readme = repoRoot.doc("README.md", "이 프로젝트는 좋습니다.")
        val many = repoRoot.doc("docs/many.md", "`App.kt`와 `Router.kt`와 `Store.kt`를 봅니다.")

        val ranked = ranker.rank(repoRoot, listOf(many, readme), index)

        ranked shouldContainExactly listOf("README.md", "docs/many.md")
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
