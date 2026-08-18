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
 * 겹침을 막는 장치가 이 조건부 갱신 하나뿐이라, 실제 MongoDB에 대고 확인합니다.
 * 조건이 조용히 빠지면 두 실행이 나란히 통과해 같은 저장소에 콜을 두 번 씁니다.
 */
@DataMongoTest
@Import(TestcontainersConfiguration::class, MongoAuditingConfiguration::class, ClockConfiguration::class)
class MongoQuizGenerationStarterTest(
    @Autowired private val mongoTemplate: MongoTemplate,
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
) {
    private val starter = MongoQuizGenerationStarter(mongoTemplate)

    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        quizRepoRepository.deleteAll()
    }

    @Test
    fun `대기 중인 저장소는 한 번만 점유된다`() {
        val quizRepo = quizRepoRepository.save(quizRepoOf())

        starter.start(quizRepo.id, TIMEOUT_AT) shouldBe true
        // 같은 저장소로 다시 들어온 실행이다. 조건이 없으면 이쪽도 통과해 콜이 두 배로 나간다.
        starter.start(quizRepo.id, TIMEOUT_AT.plusSeconds(60)) shouldBe false

        val found = quizRepoRepository.findById(quizRepo.id).orElse(null).shouldNotBeNull()
        found.status shouldBe QuizRepoStatus.STARTED
        // 시효는 먼저 잡은 쪽의 값이어야 한다. 뒤에 온 쪽이 늘려 놓으면 죽은 점유가 그만큼 오래 남는다.
        found.timeoutAt shouldBe TIMEOUT_AT
    }

    @Test
    fun `시효가 지난 점유라도 선점이 직접 빼앗지는 않는다`() {
        val quizRepo = quizRepoRepository.save(quizRepoOf())
        starter.start(quizRepo.id, Instant.EPOCH) shouldBe true

        // 시효가 다한 점유를 푸는 것은 회수(QuizGenerationReclaimer)의 일이다. 여기서 함께 빼앗으면
        // 조건이 READY 하나가 아니게 되어, 살아 있는 점유를 시계 오차만큼 앞질러 빼앗을 수 있다.
        starter.start(quizRepo.id, TIMEOUT_AT) shouldBe false
    }

    // 표시용 필드는 점유 규약과 무관해 아무 값이나 채운다.
    private fun quizRepoOf() =
        QuizRepo(
            githubRepoId = "1310710749",
            githubRepoUrl = "https://github.com/Nexters/Git-it-Server",
            name = "Git-it-Server",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
            registeredAt = Instant.EPOCH,
        )

    companion object {
        private val TIMEOUT_AT = Instant.parse("2026-08-18T00:30:00Z")
    }
}
