package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorKind
import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.repo.SourceBundler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * 앵커 후보를 코드와 대조해 확정하거나 폐기합니다.
 *
 * 문서가 거짓말한 개념이 걸러지는 지점입니다. 그럴듯한 홍보 문구에서 뽑은 개념은
 * 코드에 짚을 곳이 없어 여기서 전부 탈락합니다.
 */
@Component
class AnchorGate(
    private val sourceBundler: SourceBundler,
) {
    /**
     * 검증을 통과한 개념만 돌려줍니다.
     *
     * 앵커가 둘 미만인 개념은 폐기합니다 — 정의만 있고 쓰이는 곳이 없는 개념은 출제 가치가 없습니다.
     * 살아남은 개념이 둘 미만이면 억지로 만들지 않고 [ErrorCode.NO_CONCEPTS]로 거절합니다.
     */
    fun confirm(
        repoRoot: Path,
        selections: List<Pair<Concept, List<AnchorCandidate>>>,
    ): List<AnchoredConcept> {
        val confirmed = selections.mapNotNull { (concept, candidates) -> confirm(repoRoot, concept, candidates) }

        if (confirmed.size < MINIMUM_CONCEPTS) {
            throw BaseException(ErrorCode.NO_CONCEPTS, "앵커까지 확정된 개념이 ${confirmed.size}개입니다")
        }
        return confirmed
    }

    private fun confirm(
        repoRoot: Path,
        concept: Concept,
        candidates: List<AnchorCandidate>,
    ): AnchoredConcept? {
        val anchors = candidates.mapNotNull { verify(repoRoot, concept, it) }.distinct()

        if (anchors.size < MINIMUM_ANCHORS) {
            logger.debug { "Discarded concept '${concept.name}': ${anchors.size} anchor(s) survived" }
            return null
        }
        return AnchoredConcept(concept, anchors)
    }

    private fun verify(
        repoRoot: Path,
        concept: Concept,
        candidate: AnchorCandidate,
    ): Anchor? {
        // 콜에 넘긴 적 없는 파일이면 지어낸 것이다.
        if (candidate.file !in concept.candidatePaths) {
            logger.debug { "Discarded anchor of '${concept.name}': unknown file ${candidate.file}" }
            return null
        }

        val where = "${candidate.file}:${candidate.startLine}-${candidate.endLine}"
        val lines = sourceBundler.lines(repoRoot, candidate.file)
        if (lines == null || !candidate.fitsIn(lines.size)) {
            logger.debug { "Discarded anchor of '${concept.name}': bad range $where" }
            return null
        }

        // 라인 번호가 한두 줄 어긋나는 것은 정상이다. 정확히 그 줄을 요구하면 멀쩡한 앵커가 전멸한다.
        val symbol = candidate.symbol.trim()
        val from = (candidate.startLine - 1 - CONTEXT_LINES).coerceAtLeast(0)
        val to = (candidate.endLine + CONTEXT_LINES).coerceAtMost(lines.size)
        val matchedLine = if (symbol.isEmpty()) null else (from until to).firstOrNull { symbol in lines[it] }?.plus(1)
        if (matchedLine == null) {
            logger.debug { "Discarded anchor of '${concept.name}': symbol '$symbol' not near $where" }
            return null
        }

        // 어긋난 줄 번호를 그대로 저장하면 발췌가 심볼이 있는 줄을 비껴가, 문제 생성이 심볼 없는 코드를 보게 된다.
        val startLine = minOf(candidate.startLine, matchedLine)

        return Anchor(
            file = candidate.file,
            startLine = startLine,
            // 파일 절반을 앵커라고 우겨도 문제 생성이 읽는 양은 여기서 닫힌다.
            endLine = maxOf(minOf(candidate.endLine, startLine + MAX_SPAN_LINES - 1), matchedLine),
            kind = kindOf(candidate.kind),
            symbol = symbol,
        )
    }

    private fun AnchorCandidate.fitsIn(lineCount: Int) = startLine in 1..endLine && endLine <= lineCount

    // 모르는 라벨이라고 앵커를 버리지는 않는다. 라벨은 서빙 분류일 뿐이고 위치와 심볼은 이미 검증됐다.
    private fun kindOf(kind: String): AnchorKind =
        AnchorKind.entries.firstOrNull { it.name.equals(kind.trim(), ignoreCase = true) } ?: AnchorKind.DEFINITION

    companion object {
        private const val MINIMUM_CONCEPTS = 2
        private const val MINIMUM_ANCHORS = 2
        private const val CONTEXT_LINES = 3
        private const val MAX_SPAN_LINES = 80
    }
}
