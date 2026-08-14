package com.nexters.gitit.domain.project

/**
 * 회원이 고른 문제 난이도.
 *
 * 직급(주니어·시니어)이 아니라 프로젝트를 얼마나 깊이 파고들지로만 나눕니다.
 * [L1] 오리엔테이션급 · [L2] 동작 이해 · [L3] 설계 이해.
 */
enum class QuizLevel {
    L1,
    L2,
    L3,
}
