package com.nexters.gitit.domain.quizrepo

/**
 * 문제 저장소의 문제 생성이 어디까지 왔는지.
 *
 * 값은 진행률이 아니라 **지금 이 저장소가 무엇을 가지고 있는가**입니다. 가진 것이 달라지는 자리에만 값이 있습니다.
 *
 * [REJECTED]와 [FAILED]는 다릅니다. 앞은 "이 레포로는 문제를 못 만든다"는 판정이라 사유가 있고 다시 돌려도
 * 같은 결과지만, 뒤는 사고라 사유가 없고 다시 돌리면 성공할 수 있습니다.
 *
 * 이 enum은 실패 지점을 적는 값이기도 합니다 — [QuizRepo.failedFrom]이 [FAILED] 직전의 값을 들고 있습니다.
 */
enum class QuizRepoStatus {
    /** 생성 대기 중. 갓 등록됐거나 재시도로 다시 줄에 선 상태라, 산출물이 없다는 뜻은 아닙니다 — 앵커 유무는 [QuizRepo.failedFrom]이 알려줍니다. */
    READY,

    /** 개념·앵커는 확정됐지만 문제는 없습니다. 학습자에게 내보낼 수는 없고, 다시 만들 때 쓸 재료만 있는 상태입니다. */
    ANCHORED,

    /** 문제를 낼 수 없다고 판정했습니다. 사유는 `QuizRepo.rejectedReason`에 있습니다. */
    REJECTED,

    /** 예외로 중단됐습니다. 사유는 로그에만 있습니다. */
    FAILED,

    /** 문제까지 다 만들어졌습니다. [QuizRepo.learningSets]가 채워진 유일한 상태이고, 학습자에게 내보낼 수 있는 것도 여기뿐입니다. */
    COMPLETED,
}
