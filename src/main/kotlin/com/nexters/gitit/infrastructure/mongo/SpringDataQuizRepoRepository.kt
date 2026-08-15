package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.quizrepo.QuizRepo
import org.springframework.data.mongodb.repository.MongoRepository

interface SpringDataQuizRepoRepository : MongoRepository<QuizRepo, String> {
    fun findByGithubRepoIdAndDeletedAtIsNull(githubRepoId: String): QuizRepo?

    fun findByIdAndDeletedAtIsNull(id: String): QuizRepo?
}
