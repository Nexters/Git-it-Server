package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.infrastructure.time.ClockConfiguration
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.Instant

/**
 * 죽은 실행이 붙잡은 저장소를 되돌리는 유일한 자리입니다. 조건이 조용히 어긋나면 붙잡힌 저장소가 영영
 * 대기줄 밖에 남거나(안 걸림), 멀쩡히 돌고 있는 회차가 대기줄로 끌려 내려옵니다(너무 걸림).
 */
@DataMongoTest
@Import(TestcontainersConfiguration::class, MongoAuditingConfiguration::class, ClockConfiguration::class)
class QuizGenerationReclaimerTest(
    @Autowired private val mongoTemplate: MongoTemplate,
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
) {
    private val starter = MongoQuizGenerationStarter(mongoTemplate)

    private val reclaimer = QuizGenerationReclaimer(mongoTemplate)

    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        quizRepoRepository.deleteAll()
    }

    @Test
    fun `시효가 다한 점유는 대기줄로 돌아온다`() {
        val quizRepo = quizRepoRepository.save(quizRepoOf("1"))
        starter.start(quizRepo.id, NOW)

        reclaimer.reclaim(NOW) shouldBe 1L

        val found = quizRepoRepository.findById(quizRepo.id).orElse(null).shouldNotBeNull()
        found.status shouldBe QuizRepoStatus.READY
        // 점유가 남아 있으면 다시 집은 회차가 남의 시효를 물려받아, 자기 결과를 시작하자마자 버린다.
        found.timeoutAt shouldBe null
        // 회수는 순서를 건드리지 않는다. 기다린 만큼 앞줄에 서야 다시 집힌다.
        found.registeredAt shouldBe Instant.EPOCH
    }

    @Test
    fun `아직 시효가 남은 점유는 건드리지 않는다`() {
        val quizRepo = quizRepoRepository.save(quizRepoOf("2"))
        starter.start(quizRepo.id, NOW.plusSeconds(1))

        reclaimer.reclaim(NOW) shouldBe 0L

        quizRepoRepository
            .findById(quizRepo.id)
            .orElse(null)
            .shouldNotBeNull()
            .status shouldBe QuizRepoStatus.STARTED
    }

    // 표시용 필드는 회수 규약과 무관해 아무 값이나 채운다.
    private fun quizRepoOf(githubRepoId: String) =
        QuizRepo(
            githubRepoId = githubRepoId,
            githubRepoUrl = "https://github.com/Nexters/repo-$githubRepoId",
            name = "repo-$githubRepoId",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
            registeredAt = Instant.EPOCH,
        )

    companion object {
        private val NOW = Instant.parse("2026-08-18T00:00:00Z")
    }
}
