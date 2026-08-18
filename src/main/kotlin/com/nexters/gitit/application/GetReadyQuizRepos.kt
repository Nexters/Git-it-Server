package com.nexters.gitit.application

import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.stereotype.Service

@Service
class GetReadyQuizRepos(
    private val quizRepoRepository: QuizRepoRepository,
) {
    /**
     * 문제 생성을 기다리는 저장소를 오래 기다린 순서로 돌려줍니다. 대기 중인 것이 없으면 빈 결과입니다.
     *
     * 항목은 id뿐이고, 읽는 순간 이미 낡은 목록입니다 — 여기 있다고 아직 대기 중이라는 보장은 없습니다.
     */
    operator fun invoke(): Result = Result(items = quizRepoRepository.findAllReady().map { QuizRepoItem(quizRepoId = it.id) })

    data class Result(
        val items: List<QuizRepoItem>,
    )

    data class QuizRepoItem(
        val quizRepoId: String,
    )
}
