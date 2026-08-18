package com.nexters.gitit.domain.quizrepo

/**
 * 문제 저장소의 문제 생성이 어디까지 왔는지.
 *
 * 이 값이 답하는 것은 **지금 누가 쥐고 있는지**뿐입니다. 어디까지 갔는지는 산출물([QuizRepo.anchoredConcepts])이 답합니다.
 *
 * [REJECTED]와 [FAILED]는 다릅니다. 앞은 "이 레포로는 문제를 못 만든다"는 판정이라 사유가 있고 다시 돌려도
 * 같은 결과지만, 뒤는 사고라 사유가 없고 다시 돌리면 성공할 수 있습니다.
 */
enum class QuizRepoStatus {
    /** 생성 대기 중. 갓 등록됐거나 재시도로 다시 줄에 선 것이라, 산출물이 없다는 뜻은 아닙니다 — 앵커 유무는 [QuizRepo.anchoredConcepts]가 알려줍니다. */
    READY,

    /** 누군가 생성을 쥐고 돌리는 중. [QuizRepo.timeoutAt]이 그 점유의 시효입니다. */
    STARTED,

    /** 문제를 낼 수 없다고 판정했습니다. 사유는 [QuizRepo.rejectedReason]에 있습니다. */
    REJECTED,

    /** 예외로 중단됐습니다. 사유는 로그에만 있습니다. */
    FAILED,

    /** 문제까지 다 만들어졌습니다. [QuizRepo.learningSets]가 채워진 유일한 상태이고, 학습자에게 내보낼 수 있는 것도 여기뿐입니다. */
    COMPLETED,
}
