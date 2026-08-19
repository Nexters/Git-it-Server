package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.ai.ConceptExtractor
import com.nexters.gitit.infrastructure.repo.DocumentBundler
import com.nexters.gitit.infrastructure.repo.DocumentRanker
import com.nexters.gitit.infrastructure.repo.DocumentScanner
import com.nexters.gitit.infrastructure.repo.SourcePathIndex
import com.nexters.gitit.infrastructure.repo.SourceTreeBundler
import org.springframework.stereotype.Component
import java.nio.file.Path

/**
 * 해제된 레포를 읽어 "무엇을 물을 것인가"를 정합니다. 문제 생성 파이프라인의 첫 단계입니다.
 *
 * 콜에 실리는 것은 문서 본문과 **소스 파일 목록**입니다. 소스 본문은 한 줄도 읽지 않습니다.
 *
 * LLM 콜은 개념 추출 한 번뿐이고 읽을 문서는 코드가 고릅니다. 나머지는 전부 파일시스템 작업이라,
 * 콜 수가 레포 크기를 따라가지 않고 같은 레포에서는 같은 문서 묶음이 나갑니다.
 */
@Component
class DocumentAnalyzer(
    private val documentScanner: DocumentScanner,
    private val documentRanker: DocumentRanker,
    private val documentBundler: DocumentBundler,
    private val sourceTreeBundler: SourceTreeBundler,
    private val conceptExtractor: ConceptExtractor,
    private val conceptGate: ConceptGate,
) {
    fun analyze(repoRoot: Path): List<Concept> {
        val files = documentScanner.scan(repoRoot)
        if (files.documents.isEmpty()) {
            throw BaseException(ErrorCode.NO_CONCEPTS, "분석할 문서가 없습니다")
        }

        val index = SourcePathIndex(files.sources)
        val ranked = documentRanker.rank(repoRoot, files.documents, index)
        val bundle = documentBundler.bundle(repoRoot, ranked)

        val candidates = conceptExtractor.extract(bundle, sourceTreeBundler.bundle(files.sources))

        return conceptGate.confirm(repoRoot, files.documents, index, candidates)
    }
}
