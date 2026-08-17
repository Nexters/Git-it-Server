package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository

@Repository
class MongoQuizRepoRepository(
    private val quizRepoRepository: SpringDataQuizRepoRepository,
) : QuizRepoRepository {
    override fun saveIfAbsent(quizRepo: QuizRepo): QuizRepo =
        try {
            quizRepoRepository.save(quizRepo)
        } catch (e: DuplicateKeyException) {
            findByGithubRepoId(quizRepo.githubRepoId) ?: throw e
        }

    override fun findById(id: String): QuizRepo? = quizRepoRepository.findByIdAndDeletedAtIsNull(id)

    override fun findAllByIds(ids: Collection<String>): List<QuizRepo> = quizRepoRepository.findAllByIdInAndDeletedAtIsNull(ids)

    override fun findAllPending(): List<QuizRepo> =
        quizRepoRepository.findAllByStatusAndDeletedAtIsNullOrderByRegisteredAtAsc(QuizRepoStatus.READY)

    override fun save(quizRepo: QuizRepo): QuizRepo = quizRepoRepository.save(quizRepo)

    private fun findByGithubRepoId(githubRepoId: String): QuizRepo? = quizRepoRepository.findByGithubRepoIdAndDeletedAtIsNull(githubRepoId)
}
