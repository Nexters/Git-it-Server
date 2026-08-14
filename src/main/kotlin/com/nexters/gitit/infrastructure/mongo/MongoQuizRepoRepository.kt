package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.stereotype.Repository

@Repository
class MongoQuizRepoRepository(
    private val quizRepoRepository: SpringDataQuizRepoRepository,
) : QuizRepoRepository {
    override fun save(quizRepo: QuizRepo): QuizRepo = quizRepoRepository.save(quizRepo)

    override fun findByGithubRepoId(githubRepoId: String): QuizRepo? = quizRepoRepository.findByGithubRepoIdAndDeletedAtIsNull(githubRepoId)
}
