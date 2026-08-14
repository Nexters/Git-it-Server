package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository

@Repository
class MongoProjectRepository(
    private val projectRepository: SpringDataProjectRepository,
) : ProjectRepository {
    /**
     * 회원과 저장소 조합에 걸린 유니크 인덱스를 최종 판정자로 씁니다. 요청이 연달아 들어와도 프로젝트가 둘로 늘지 않고,
     * 밀린 쪽은 이긴 쪽 도큐먼트를 다시 읽어 돌려줍니다.
     */
    override fun saveIfAbsent(project: Project): Project =
        findByMemberIdAndQuizRepoId(project.memberId, project.quizRepoId)
            ?: try {
                projectRepository.save(project)
            } catch (e: DuplicateKeyException) {
                findByMemberIdAndQuizRepoId(project.memberId, project.quizRepoId) ?: throw e
            }

    private fun findByMemberIdAndQuizRepoId(
        memberId: String,
        quizRepoId: String,
    ): Project? = projectRepository.findByMemberIdAndQuizRepoIdAndDeletedAtIsNull(memberId, quizRepoId)
}
