package com.nexters.gitit.domain.quizrepo

interface QuizRepoRepository {
    fun save(quizRepo: QuizRepo): QuizRepo

    fun findByGithubRepoId(githubRepoId: String): QuizRepo?
}
