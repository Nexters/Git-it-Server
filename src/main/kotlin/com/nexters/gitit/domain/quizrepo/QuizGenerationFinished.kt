package com.nexters.gitit.domain.quizrepo

/**
 * 문제 생성이 더 돌지 않는 상태에 닿았음을 알립니다. 성공도 거절도 사고도 전부 여기로 나옵니다.
 *
 * 결과를 싣지도, 성공·실패로 이벤트를 나누지도 않습니다. 알림 문구는 [QuizRepo.status]와
 * [QuizRepo.rejectedReason]에서 갈리는데 그 값은 이미 도큐먼트에 있어서, 나눠 봐야 받는 쪽이
 * 배선을 두 벌 들고도 결국 상태를 다시 읽습니다.
 */
data class QuizGenerationFinished(
    val quizRepoId: String,
)
