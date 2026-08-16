package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.ai.AnchorSelector
import com.nexters.gitit.infrastructure.repo.SourceBundler
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * 개념마다 후보 파일을 읽혀 앵커를 고르고, 코드와 대조해 확정합니다.
 *
 * 콜은 개념당 한 번입니다 — 콜 수가 레포 크기가 아니라 개념 수의 함수여야 비용이 예측됩니다.
 * 콜이 실패한 개념은 버리고 나머지로 진행합니다.
 */
@Component
class AnchorLocator(
    private val sourceBundler: SourceBundler,
    private val anchorSelector: AnchorSelector,
    private val anchorGate: AnchorGate,
) {
    fun locate(
        repoRoot: Path,
        concepts: List<Concept>,
    ): List<AnchoredConcept> {
        val selections =
            concepts
                .inParallel { concept ->
                    runCatching { concept to anchorSelector.select(concept, sourceBundler.bundle(repoRoot, concept.candidatePaths)) }
                }.successesOrThrow()

        return anchorGate.confirm(repoRoot, selections)
    }
}
