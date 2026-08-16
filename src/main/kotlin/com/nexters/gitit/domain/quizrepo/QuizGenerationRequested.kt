package com.nexters.gitit.domain.quizrepo

/**
 * 문제를 채워 넣어야 할 저장소가 새로 생겼음을 알립니다.
 *
 * 생성은 수 분이 걸려 요청 스레드에서 끝낼 수 없으므로, 등록은 여기서 끊고 실제 생성은 이 이벤트를 받는 쪽이 맡습니다.
 * 스냅숏 대신 식별자만 싣는 이유는 수신 시점에 상태가 이미 달라져 있을 수 있어, 받는 쪽이 다시 읽는 편이 안전해서입니다.
 */
data class QuizGenerationRequested(
    val quizRepoId: String,
)
