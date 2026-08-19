package com.nexters.gitit.infrastructure.repo

import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * 어떤 문서를 먼저 읽힐지 정합니다. 점수는 그 문서가 언급한 실존 소스 파일의 수입니다.
 *
 * 코드에서 근거를 찾지 못하는 문서에서 뽑은 개념은 뒷단계 검증에서 전멸하므로, 그 판정을 콜 전에
 * 근사해 순서만 바꿉니다. 뒤로 밀린다고 가치 없는 문서라는 뜻은 아닙니다.
 */
@Component
class DocumentRanker {
    /**
     * 고정 1순위 → 점수 내림차순 → 상대 경로 사전순으로 정렬해 돌려줍니다.
     *
     * 동점을 경로로 깨야 같은 레포에서 항상 같은 순서가 나오고, 그래야 번들도 같아집니다.
     */
    fun rank(
        repoRoot: Path,
        documents: List<String>,
        index: SourcePathIndex,
    ): List<String> {
        val scores = documents.associateWith { score(repoRoot.resolve(it), index) }

        return documents.sortedWith(
            compareByDescending<String> { isPinned(it) }
                .thenByDescending { scores.getValue(it) }
                .thenBy { it },
        )
    }

    /**
     * 점수와 무관하게 맨 앞에 두어야 하는 문서인지 판정합니다. `docs/adr/` 아래와 [PINNED_NAMES]가 대상입니다.
     *
     * 점수는 파일명을 잔뜩 언급하는 설치 가이드를 과대평가하고 산문형 설계 문서를 과소평가합니다.
     * 이 목록이 그 위음성을 막습니다.
     */
    private fun isPinned(document: String): Boolean {
        if (document.lowercase().startsWith("docs/adr/")) return true

        val baseName = document.substringAfterLast('/').substringBeforeLast('.').uppercase()
        return baseName in PINNED_NAMES
    }

    private fun score(
        document: Path,
        index: SourcePathIndex,
    ): Int =
        CODE_REFERENCE
            .findAll(document.readText())
            .map { it.value }
            .distinct()
            .count { index.resolve(it) != null }

    companion object {
        private val PINNED_NAMES = setOf("README", "ARCHITECTURE", "DESIGN", "RATIONALE")

        // `/`가 낀 경로 꼴이나 `이름.확장자` 꼴. 오탐은 인덱스가 걸러주므로 스톱워드 목록이 필요 없다.
        private val CODE_REFERENCE = Regex("""[\w.\-/]+/[\w.\-]+\.\w+|[\w\-]+\.[A-Za-z]\w{0,9}""")
    }
}
