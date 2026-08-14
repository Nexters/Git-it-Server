package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.repo.SourcePathIndex
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {}

/**
 * 개념 후보를 검증해 확정하거나 폐기합니다.
 *
 * 문서 근거가 진짜인지 검사하는 지점은 파이프라인 전체에서 여기뿐입니다.
 * 뒷단계는 코드와 식별자만 보므로, 지어낸 근거는 여기를 지나면 영영 걸리지 않습니다.
 */
@Component
class ConceptGate {
    /**
     * 검증을 통과한 개념만 돌려줍니다.
     *
     * 후보는 셋 중 하나에 걸리면 조용히 폐기됩니다 — 모르는 `sourceDoc`,
     * 그 문서에 실재하지 않는 인용, 실재 파일로 확정되지 않는 경로 힌트.
     * 통과한 개념이 둘 미만이면 억지로 만들지 않고 [ErrorCode.NO_CONCEPTS]로 거절합니다.
     */
    fun confirm(
        repoRoot: Path,
        documents: List<String>,
        index: SourcePathIndex,
        candidates: List<ConceptCandidate>,
    ): List<Concept> {
        val documentTexts = documents.associateWith { repoRoot.resolve(it).readText() }
        val confirmed = candidates.mapNotNull { confirm(it, documentTexts, index) }

        if (confirmed.size < MINIMUM_CONCEPTS) {
            throw BaseException(ErrorCode.NO_CONCEPTS, "검증을 통과한 개념이 ${confirmed.size}개입니다")
        }
        return confirmed
    }

    private fun confirm(
        candidate: ConceptCandidate,
        documentTexts: Map<String, String>,
        index: SourcePathIndex,
    ): Concept? {
        val sourceText = documentTexts[candidate.sourceDoc]
        if (sourceText == null) {
            logger.debug { "Discarded concept '${candidate.name}': unknown sourceDoc ${candidate.sourceDoc}" }
            return null
        }

        // 원문 복붙을 요구한 이유가 이 대조다. 요약을 받았다면 여기서 걸린다.
        if (!normalize(sourceText).contains(normalize(candidate.rationale))) {
            logger.debug { "Discarded concept '${candidate.name}': rationale not found in ${candidate.sourceDoc}" }
            return null
        }

        // 경로 확정은 코드가 한다. 필터가 사후 청소에서 사전 생성으로 바뀌어 지어낼 여지가 사라진다.
        val candidatePaths = candidate.pathHints.mapNotNull { index.resolve(it) }.distinct()
        if (candidatePaths.isEmpty()) {
            logger.debug { "Discarded concept '${candidate.name}': no path hint resolved" }
            return null
        }

        return Concept(
            name = candidate.name,
            rationale = candidate.rationale,
            sourceDoc = candidate.sourceDoc,
            candidatePaths = candidatePaths,
        )
    }

    // 줄바꿈이나 들여쓰기만 다른 인용을 통과시키기 위해 공백을 하나로 접습니다.
    private fun normalize(text: String) = text.replace(WHITESPACE, " ").trim()

    companion object {
        private const val MINIMUM_CONCEPTS = 2
        private val WHITESPACE = Regex("""\s+""")
    }
}
