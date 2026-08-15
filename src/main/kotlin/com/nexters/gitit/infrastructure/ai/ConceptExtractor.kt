package com.nexters.gitit.infrastructure.ai

import com.nexters.gitit.infrastructure.quiz.ConceptCandidate

/**
 * 문서 번들과 소스 파일 목록에서 개념 후보를 뽑는 단 한 번의 LLM 콜.
 *
 * 파이프라인 안쪽 이음새라 도메인 포트가 아닙니다. 분리한 이유는 두 가지입니다.
 * 테스트가 콜 없이 문서 분석을 돌려야 하고,
 * 나중에 검증용으로 생성과 다른 모델을 붙일 자리가 실제로 생깁니다.
 */
fun interface ConceptExtractor {
    fun extract(
        bundle: String,
        sourceTree: String,
    ): List<ConceptCandidate>
}
