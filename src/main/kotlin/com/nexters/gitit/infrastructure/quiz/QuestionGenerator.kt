package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.quizrepo.AnchoredConcept
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.LearningSet
import com.nexters.gitit.infrastructure.ai.QuestionWriter
import com.nexters.gitit.infrastructure.repo.SourceBundler
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * 개념마다 앵커 본문을 읽혀 학습 세트를 쓰게 하고, 형식을 검사해 확정합니다.
 *
 * 콜은 개념 × 레벨입니다 (이유는 [QuestionWriter]). 레벨끼리 서로가 낸 문제를 못 보므로
 * 중복 방지는 프롬프트의 레벨 정의에만 기댑니다.
 */
@Component
class QuestionGenerator(
    private val sourceBundler: SourceBundler,
    private val questionWriter: QuestionWriter,
    private val questionGate: QuestionGate,
) {
    fun generate(
        repoRoot: Path,
        anchored: List<AnchoredConcept>,
    ): List<LearningSet> {
        val bundles = anchored.map { sourceBundler.excerpt(repoRoot, it.anchors) }
        val tasks = anchored.indices.flatMap { index -> Depth.entries.map { index to it } }

        val written = tasks.inParallel { (index, depth) -> index to questionWriter.write(anchored[index].concept, bundles[index], depth) }

        val drafts =
            anchored.mapIndexed { index, concept ->
                concept to merge(written.filter { it.first == index }.map { it.second })
            }

        return questionGate.confirm(drafts)
    }

    /** orientation과 앵커 요약은 어느 레벨에서 받아도 같은 개념을 설명하므로 첫 콜의 것만 씁니다. */
    private fun merge(parts: List<LearningSetDraft>) =
        LearningSetDraft(
            orientation = parts.first().orientation,
            anchorSummaries = parts.first().anchorSummaries,
            questions = parts.flatMap { it.questions },
        )
}
