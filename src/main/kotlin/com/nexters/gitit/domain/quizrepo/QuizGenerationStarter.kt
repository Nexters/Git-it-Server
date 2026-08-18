package com.nexters.gitit.domain.quizrepo

import java.time.Instant

/**
 * 대기 중인 저장소 하나를 문제 생성 실행자에게 넘깁니다.
 *
 * 조건 검사와 쓰기가 한 번의 원자적 갱신이어야 합니다 — 읽고 나서 쓰면 그 사이에 들어온 실행도 함께
 * 통과해 같은 저장소에 LLM 콜이 두 번 나갑니다.
 */
fun interface QuizGenerationStarter {
    /**
     * [quizRepoId]가 대기 중이면 [timeoutAt]까지 유효한 점유로 바꾸고 true, 이미 남이 쥐고 있거나
     * 대기 중이 아니면 false를 돌려줍니다.
     */
    fun start(
        quizRepoId: String,
        timeoutAt: Instant,
    ): Boolean
}
