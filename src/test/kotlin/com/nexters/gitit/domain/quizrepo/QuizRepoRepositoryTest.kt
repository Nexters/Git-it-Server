package com.nexters.gitit.domain.quizrepo

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.infrastructure.mongo.MongoAuditingConfiguration
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import com.nexters.gitit.infrastructure.time.ClockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DuplicateKeyException

/**
 * 등록 유스케이스가 동시 요청을 이 유니크 인덱스에 맡기고 있으므로, 인덱스가 실제로 걸리는지를 여기서 못 박습니다.
 * 인덱스가 조용히 빠지면 유스케이스의 중복 처리 분기는 영영 실행되지 않고 같은 저장소가 둘로 생깁니다.
 */
@DataMongoTest
@Import(TestcontainersConfiguration::class, MongoAuditingConfiguration::class, ClockConfiguration::class)
class QuizRepoRepositoryTest(
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
) {
    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        quizRepoRepository.deleteAll()
    }

    @Test
    fun `같은 githubRepoId로 두 번 저장하면 두 번째가 실패한다`() {
        quizRepoRepository.save(quizRepoOf(githubRepoUrl = "https://github.com/Nexters/first"))

        // id를 재사용하면 같은 도큐먼트의 갱신이 되어버리므로, 별개의 인스턴스로 저장한다.
        shouldThrow<DuplicateKeyException> {
            quizRepoRepository.save(quizRepoOf(githubRepoUrl = "https://github.com/Nexters/second"))
        }

        quizRepoRepository
            .findByGithubRepoIdAndDeletedAtIsNull(GITHUB_REPO_ID)
            .shouldNotBeNull()
            .githubRepoUrl shouldBe "https://github.com/Nexters/first"
    }

    private fun quizRepoOf(githubRepoUrl: String) = QuizRepo(githubRepoId = GITHUB_REPO_ID, githubRepoUrl = githubRepoUrl)

    companion object {
        private const val GITHUB_REPO_ID = "1310710749"
    }
}
