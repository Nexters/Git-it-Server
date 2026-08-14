package com.nexters.gitit.infrastructure.ai

import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.infrastructure.quiz.AnchorCandidate

/**
 * 개념 하나의 후보 파일에서 앵커를 고르는 콜. 개념당 한 번만 호출됩니다.
 *
 * 심볼 인덱스가 없어 "앵커를 찾아내라"가 아니라 "이 파일들에서 골라라"로 묻습니다.
 * 그래서 무엇을 고르게 하느냐보다 [bundle]에 무엇을 실었느냐가 결과를 좌우합니다.
 *
 * [ConceptExtractor]와 같은 이유로 인터페이스입니다 — 테스트가 콜 없이 단계 전체를 돌려야 합니다.
 */
fun interface AnchorSelector {
    fun select(
        concept: Concept,
        bundle: String,
    ): List<AnchorCandidate>
}
