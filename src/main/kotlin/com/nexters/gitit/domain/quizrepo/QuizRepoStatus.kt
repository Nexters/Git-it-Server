package com.nexters.gitit.domain.quizrepo

/**
 * 문제 저장소의 문제 생성이 어디까지 왔는지. 등록 직후가 [READY], 문제가 다 만들어지면 [COMPLETED],
 * 문제를 낼 수 없다고 판정되면 [REJECTED]입니다.
 *
 * 값이 셋뿐인 것은 생성 중간 단계(문서 분석·앵커·생성 등)에 아직 이름을 붙이지 않았기 때문입니다.
 * 파이프라인이 붙으면 [READY]와 [COMPLETED] 사이에 값이 여럿 끼어들 예정이라, 상태를 판정할 때는
 * 해당하는 값을 나열하기보다 "무엇이 아닌가"로 거르는 쪽이 새 값에 안 깨집니다.
 */
enum class QuizRepoStatus {
    READY,
    REJECTED,
    COMPLETED,
}
