package com.nexters.gitit.domain.project

import java.time.Instant

/**
 * 회원이 문제 하나에 대해 마지막으로 낸 답. 같은 문제를 다시 풀면 이전 것을 지우고 이걸로 갈아끼웁니다.
 *
 * 객관식만 정답 여부를 갖습니다. 서술형은 학습자 자가채점이라 서버가 채점 결과를 모릅니다.
 *
 * [answeredAt]은 답변마다 있습니다. 덮어쓰기라 도큐먼트의 수정 시각으로는 마지막 한 건밖에 알 수 없습니다.
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
