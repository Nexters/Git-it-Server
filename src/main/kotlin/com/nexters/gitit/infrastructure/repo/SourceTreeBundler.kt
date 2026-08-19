package com.nexters.gitit.infrastructure.repo

import org.springframework.stereotype.Component

/**
 * 개념 추출 콜에 함께 실을 소스 파일 **목록**을 만듭니다. 내용은 한 글자도 싣지 않습니다.
 *
 * 문서가 파일 경로를 짚어 주는 레포는 드물어서, 목록이 함께 가야 경로를 짚지 않는 문서에서도
 * 개념이 살아남습니다.
 *
 * 목록뿐이라 콜 비용이 레포 내용 크기가 아니라 파일 수에 비례하고, 그마저 [BUDGET_CHARS]로 잘립니다.
 * 경로 확정은 여기가 아니라 [SourcePathIndex]가 합니다.
 */
@Component
class SourceTreeBundler {
    /**
     * 얕은 경로부터 예산이 닿는 데까지 채워 줄바꿈으로 잇습니다. 예산을 넘는 나머지는 빠집니다.
     *
     * 깊이순 정렬이 잘릴 때 무엇이 남는지를 정합니다. 사전순으로 자르면 `.github/workflows/…`가
     * 먼저 들어차고 정작 `src/` 아래가 통째로 빠집니다.
     */
    fun bundle(sources: List<String>): String {
        val builder = StringBuilder()
        var remaining = BUDGET_CHARS

        for (path in sources.filterNot(::isNoise).sortedWith(SHALLOW_FIRST)) {
            if (path.length + 1 > remaining) break

            builder.append(path).append('\n')
            remaining -= path.length + 1
        }

        return builder.toString().trimEnd()
    }

    /**
     * 학습할 대상이 아닌 파일을 뺍니다. 확장자가 아니라 [NOISE_SEGMENTS]에 든 디렉터리 이름만 봅니다.
     *
     * 테스트·의존성 사본·빌드 산출물은 개념의 근거가 되지 않으면서 파일 수로는 본체를 압도합니다.
     */
    private fun isNoise(path: String): Boolean = NOISE_SEGMENTS.any { "/$it/" in "/$path" }

    companion object {
        // 문서 번들과 합쳐 개념 추출 콜 하나에 실린다. 여기가 크면 콜 비용이 레포 크기를 따라간다.
        private const val BUDGET_CHARS = 40_000

        private val NOISE_SEGMENTS =
            setOf(
                "test",
                "tests",
                "spec",
                "specs",
                "node_modules",
                "vendor",
                "third_party",
                "build",
                "dist",
                "target",
                "fixtures",
                "testdata",
            )

        private val SHALLOW_FIRST = compareBy<String>({ it.count { char -> char == '/' } }, { it })
    }
}
