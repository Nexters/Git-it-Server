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
 * 해제된 레포를 읽어 "무엇을 물을 것인가"를 정합니다. 코드 본문은 한 줄도 읽지 않습니다.
 *
 * 유스케이스가 아니라 문제 생성 파이프라인의 첫 단계입니다 —
 * [com.nexters.gitit.application.GenerateQuiz]가 레포 수집 직후에 호출합니다.
 *
 * 콜에 실리는 것은 문서 본문과 **소스 파일 목록**입니다. 목록을 같이 주는 이유는 문서가 파일 경로를
 * 짚어 준다는 전제가 흔히 틀리기 때문입니다. 소스 본문은 여기서 한 줄도 읽지 않습니다.
 *
 * LLM 콜은 개념 추출 한 번뿐이고 나머지는 전부 파일시스템 작업입니다.
 * 읽을 문서를 LLM이 직접 고르게 하지 않는 이유가 여기 있습니다 —
 * 도구를 쥐여주면 콜 수가 레포 크기를 따라가고, 같은 레포에서도 매번 다른 파일을 읽어
 * 결과가 재현되지 않습니다.
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
