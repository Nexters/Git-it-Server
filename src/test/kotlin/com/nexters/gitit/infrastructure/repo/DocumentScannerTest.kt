package com.nexters.gitit.infrastructure.repo

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class DocumentScannerTest {
    @TempDir
    lateinit var repoRoot: Path

    private val scanner = DocumentScanner()

    @Test
    fun `문서와 소스를 가르고 상대 경로 사전순으로 돌려준다`() {
        repoRoot.file("README.md", "# 소개")
        repoRoot.file("docs/design.md", "# 설계")
        repoRoot.file("src/App.kt", "class App")
        repoRoot.file("build.gradle.kts", "plugins {}")

        val files = scanner.scan(repoRoot)

        files.documents shouldContainExactly listOf("README.md", "docs/design.md")
        files.sources shouldContainExactly listOf("build.gradle.kts", "src/App.kt")
    }

    @Test
    fun `번역본과 생성된 문서는 제외한다`() {
        repoRoot.file("README.md", "# 소개")
        repoRoot.file("README.zh-CN.md", "# 介绍")
        repoRoot.file("docs/ja/guide.md", "# 案内")
        repoRoot.file("apidocs/index.md", "# API")
        repoRoot.file("CHANGELOG.md", "# 변경 이력")

        scanner.scan(repoRoot).documents shouldContainExactly listOf("README.md")
    }

    private fun Path.file(
        relative: String,
        text: String,
    ) = resolve(relative).also {
        it.parent.createDirectories()
        it.writeText(text)
    }
}
