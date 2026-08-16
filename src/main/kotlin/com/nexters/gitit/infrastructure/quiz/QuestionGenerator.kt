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
 *
 * 콜이 실패해 레벨이 덜 온 개념은 버리고 나머지로 완성합니다.
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

        val written =
            tasks
                .inParallel { (index, depth) ->
                    runCatching { index to questionWriter.write(anchored[index].concept, bundles[index], depth) }
                }.successesOrThrow()
                .groupBy({ it.first }, { it.second })

        val drafts =
            anchored.mapIndexedNotNull { index, concept ->
                val parts = written[index].orEmpty()
                // 레벨이 하나라도 빠졌으면 세트를 만들지 않는다. 세 레벨이 같은 분량이어야 한다는 규칙을 이미 어긴 상태다.
                if (parts.size < Depth.entries.size) null else concept to merge(parts)
            }

        return questionGate.confirm(drafts)
    }

    /** 문제를 뺀 나머지는 어느 레벨에서 받아도 같은 개념을 설명하므로 첫 콜의 것만 씁니다. */
    private fun merge(parts: List<LearningSetDraft>) =
        LearningSetDraft(
            title = parts.first().title,
            description = parts.first().description,
            orientation = parts.first().orientation,
            anchorSummaries = parts.first().anchorSummaries,
            questions = parts.flatMap { it.questions },
        )
}
