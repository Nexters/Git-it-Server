package com.nexters.gitit.application

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.QuizLevel
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.domain.quizrepo.started
import com.nexters.gitit.infrastructure.mongo.SpringDataProjectRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Instant

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class GetQuizGenerationStatusTest(
    @Autowired private val getQuizGenerationStatus: GetQuizGenerationStatus,
    @Autowired private val projectRepository: SpringDataProjectRepository,
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
) {
    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        projectRepository.deleteAll()
        quizRepoRepository.deleteAll()
    }

    @Test
    fun `프로젝트가 보고 있는 저장소의 상태를 저장된 그대로 돌려준다`() {
        val quizRepo = quizRepoRepository.save(quizRepoOf().started())
        val project = projectRepository.save(Project(MEMBER_ID, quizRepo.id, QuizLevel.L2))

        val result = getQuizGenerationStatus(GetQuizGenerationStatus.Command(MEMBER_ID, project.id))

        result.status shouldBe QuizRepoStatus.STARTED
    }

    @Test
    fun `남의 프로젝트는 찾을 수 없다고 답한다`() {
        val quizRepo = quizRepoRepository.save(quizRepoOf())
        val project = projectRepository.save(Project(MEMBER_ID, quizRepo.id, QuizLevel.L2))

        val exception = shouldThrow<BaseException> { getQuizGenerationStatus(GetQuizGenerationStatus.Command("member-2", project.id)) }

        exception.errorCode shouldBe ErrorCode.PROJECT_NOT_FOUND
    }

    private fun quizRepoOf() =
        QuizRepo(
            githubRepoId = "repo-1",
            githubRepoUrl = "https://github.com/Nexters/Git-it-Server",
            name = "Git-it-Server",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
            registeredAt = Instant.EPOCH,
        )

    companion object {
        private const val MEMBER_ID = "member-1"
    }
}
