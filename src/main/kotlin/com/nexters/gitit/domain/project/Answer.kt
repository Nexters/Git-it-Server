package com.nexters.gitit.domain.project

import java.time.Instant

/**
 * 회원이 문제 하나에 대해 마지막으로 낸 답. 같은 문제를 다시 풀면 이전 것을 지우고 이걸로 갈아끼웁니다.
 *
 * 객관식과 서술형을 한 타입으로 뭉치지 않은 이유는 채점 가능 여부가 다르기 때문입니다 — 서술형은 학습자
 * 자가채점이라 서버가 정답 여부를 모릅니다. 하나로 합치면 정답 여부가 nullable이 되어
 * "채점되지 않은 객관식"이라는, 있을 수 없는 상태가 타입에 생깁니다.
 *
 * [answeredAt]을 답변마다 갖는 이유는 덮어쓰기라 도큐먼트의 수정 시각으로는 마지막 한 건만 알 수 있어서입니다.
 */
sealed class Answer {
    abstract val questionId: String
    abstract val answeredAt: Instant

    /**
     * [correct]를 제출 시점에 굳혀 둡니다. 조회할 때마다 문제를 다시 열어 정답과 대조하지 않아도 되고,
     * 문제가 다시 생성돼도 id가 같으면 그때 푼 결과가 그대로 유효합니다.
     */
    data class Choice(
        override val questionId: String,
        override val answeredAt: Instant,
        val selectedIndex: Int,
        val correct: Boolean,
    ) : Answer()

    data class Essay(
        override val questionId: String,
        override val answeredAt: Instant,
        val text: String,
    ) : Answer()
}
