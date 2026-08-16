package com.nexters.gitit.infrastructure.repo

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class SourceTreeBundlerTest {
    @Test
    fun `테스트와 의존성 사본을 빼고 얕은 경로부터 싣는다`() {
        val sources =
            listOf(
                ".github/workflows/ci.yml",
                "build.gradle.kts",
                "src/main/java/Repo/MongoTemplate.java",
                "src/test/java/Repo/MongoTemplateTests.java",
                "node_modules/left-pad/index.js",
            )

        val bundle = SourceTreeBundler().bundle(sources)

        // 잘릴 때 무엇이 남는지가 목록의 가치를 정한다. 깊이순이라 빌드 스크립트가 소스보다 앞이다.
        bundle.lines() shouldContainExactly
            listOf(
                "build.gradle.kts",
                ".github/workflows/ci.yml",
                "src/main/java/Repo/MongoTemplate.java",
            )
    }
}
