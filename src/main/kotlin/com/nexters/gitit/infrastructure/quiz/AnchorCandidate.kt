package com.nexters.gitit.infrastructure.quiz

/**
 * 앵커 선택 콜이 돌려준 그대로의 후보. 아직 아무것도 검증되지 않았습니다.
 *
 * 게이트를 통과하기 전까지만 사는 타입이라 저장되지 않고, 파이프라인 밖으로도 나가지 않습니다.
 *
 * [kind]를 [com.nexters.gitit.domain.quizrepo.AnchorKind]가 아니라 문자열로 받는 이유는,
 * 라벨 하나가 어긋났을 때 응답 전체의 파싱이 깨지지 않게 하기 위해서입니다.
 * 라벨은 서빙용 분류일 뿐이라 그 오류 비용이 앵커를 통째로 잃는 비용보다 훨씬 쌉니다.
 */
data class AnchorCandidate(
    val file: String,
    val startLine: Int,
    val endLine: Int,
    val kind: String,
    val symbol: String,
)
