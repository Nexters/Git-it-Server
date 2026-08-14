package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository

@Repository
class MongoQuizRepoRepository(
    private val quizRepoRepository: SpringDataQuizRepoRepository,
) : QuizRepoRepository {
    /**
     * 조회와 저장 사이에 다른 요청이 끼어들 수 있어 githubRepoId 유니크 인덱스를 최종 판정자로 씁니다.
     * 저장에서 밀린 쪽은 이긴 쪽 도큐먼트를 다시 읽어 돌려주므로, 호출부에는 경합이 드러나지 않습니다.
     */
    override fun saveIfAbsent(quizRepo: QuizRepo): QuizRepo =
        findByGithubRepoId(quizRepo.githubRepoId)
            ?: try {
                quizRepoRepository.save(quizRepo)
            } catch (e: DuplicateKeyException) {
                findByGithubRepoId(quizRepo.githubRepoId) ?: throw e
            }

    private fun findByGithubRepoId(githubRepoId: String): QuizRepo? = quizRepoRepository.findByGithubRepoIdAndDeletedAtIsNull(githubRepoId)
}
