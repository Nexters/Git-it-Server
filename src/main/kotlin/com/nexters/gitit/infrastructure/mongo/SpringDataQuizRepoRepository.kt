package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import org.springframework.data.mongodb.repository.MongoRepository

interface SpringDataQuizRepoRepository : MongoRepository<QuizRepo, String> {
    fun findByGithubRepoIdAndDeletedAtIsNull(githubRepoId: String): QuizRepo?

    fun findByIdAndDeletedAtIsNull(id: String): QuizRepo?

    fun findAllByIdInAndDeletedAtIsNull(ids: Collection<String>): List<QuizRepo>

    fun findAllByStatusAndDeletedAtIsNullOrderByRegisteredAtAsc(status: QuizRepoStatus): List<QuizRepo>
}
