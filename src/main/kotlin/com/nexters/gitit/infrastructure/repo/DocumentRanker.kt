package com.nexters.gitit.infrastructure.repo

import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * 어떤 문서를 먼저 읽힐지 정합니다. 점수는 그 문서가 언급한 실존 소스 파일의 수입니다.
 *
 * "좋은 문서 고르기"가 아니라 하류 생존율 예측입니다.
 * 코드에서 근거를 찾지 못하는 문서에서 뽑은 개념은 어차피 뒷단계 검증에서 전멸하므로,
 * 그 판정을 콜 전에 근사해 순서만 바꿉니다. 여기서 뒤로 밀리는 문서가 가치 없는 문서라는
 * 뜻은 아니고, 이 파이프라인이 진위를 검사할 수 없는 문서라는 뜻입니다.
 */
@Component
class DocumentRanker {
    /**
     * 고정 1순위 → 점수 내림차순 → 상대 경로 사전순으로 정렬해 돌려줍니다.
     *
     * 동점을 경로로 깨는 이유는 재현성입니다. 같은 레포에서 항상 같은 순서가 나와야
     * 번들이 같아지고, 번들이 같아야 캐시가 의미를 가집니다.
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
     * 점수와 무관하게 맨 앞에 두어야 하는 문서인지 판정합니다.
     *
     * 히트 수는 파일명을 잔뜩 언급하는 설치 가이드를 과대평가하고,
     * 실제 설계 의도를 담은 산문형 문서를 과소평가합니다.
     * 고정 1순위가 그 위음성을 막는 유일한 장치라, 파일명 관습을 완전히 버리지 않습니다.
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
