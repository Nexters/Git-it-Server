package com.nexters.gitit.infrastructure.repo

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SourcePathIndexTest {
    private val index =
        SourcePathIndex(
            listOf(
                "src/main/kotlin/com/nexters/gitit/GitItApplication.kt",
                "src/main/resources/application.yaml",
                "build.gradle.kts",
            ),
        )

    @Test
    fun `정확한 경로와 접미사와 유일한 파일명을 모두 해석한다`() {
        index.resolve("build.gradle.kts") shouldBe "build.gradle.kts"
        index.resolve("gitit/GitItApplication.kt") shouldBe "src/main/kotlin/com/nexters/gitit/GitItApplication.kt"
        index.resolve("`application.yaml`") shouldBe "src/main/resources/application.yaml"
    }
}
