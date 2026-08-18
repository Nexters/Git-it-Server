package com.nexters.gitit.application

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.QuizLevel
import com.nexters.gitit.domain.quizrepo.GithubRepository
import com.nexters.gitit.domain.quizrepo.GithubRepositoryResolver
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.domain.quizrepo.rejected
import com.nexters.gitit.infrastructure.mongo.SpringDataProjectRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class RegisterProjectTest(
    @Autowired private val registerProject: RegisterProject,
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
    @Autowired private val projectRepository: SpringDataProjectRepository,
) {
    // 실제 구현은 GitHub API를 호출하므로 테스트에서 진짜로 돌릴 수 없다. MongoDB는 실제 구현을 쓴다.
    @MockitoBean
    private lateinit var githubRepositoryResolver: GithubRepositoryResolver

    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        quizRepoRepository.deleteAll()
        projectRepository.deleteAll()
        given(githubRepositoryResolver.resolve(REPO_URL)).willReturn(REPO)
    }

    @Test
    fun `처음 등록하면 문제 저장소를 만들어 대기줄에 세우고 그 난이도로 프로젝트를 만든다`() {
        val result = registerProject(commandOf(memberId = "member-1", quizLevel = QuizLevel.L2))

        result.status shouldBe QuizRepoStatus.READY

        val quizRepo = quizRepoRepository.findByGithubRepoIdAndDeletedAtIsNull(REPO_ID).shouldNotBeNull()
        quizRepo.githubRepoUrl shouldBe REPO_URL

        val project = projectOf("member-1")
        project.id shouldBe result.projectId
        project.quizRepoId shouldBe quizRepo.id
        project.quizLevel shouldBe QuizLevel.L2

        // 저장이 곧 대기줄 등록이다. 스케줄러가 이 상태를 보고 집어 간다.
        quizRepoRepository
            .findAllByStatusAndDeletedAtIsNullOrderByRegisteredAtAsc(QuizRepoStatus.READY)
            .map { it.id } shouldBe listOf(quizRepo.id)
    }

    @Test
    fun `다른 회원이 같은 저장소를 등록하면 문제 저장소는 그대로 두고 프로젝트만 추가한다`() {
        val first = registerProject(commandOf(memberId = "member-1", quizLevel = QuizLevel.L2))

        val second = registerProject(commandOf(memberId = "member-2", quizLevel = QuizLevel.L3))

        second.projectId shouldNotBe first.projectId
        quizRepoRepository.count() shouldBe 1
        projectRepository.count() shouldBe 2
    }

    @Test
    fun `같은 회원이 난이도를 바꿔 다시 등록해도 기존 프로젝트를 그대로 둔다`() {
        val first = registerProject(commandOf(memberId = "member-1", quizLevel = QuizLevel.L2))

        val second = registerProject(commandOf(memberId = "member-1", quizLevel = QuizLevel.L3))

        second.projectId shouldBe first.projectId
        projectRepository.count() shouldBe 1
        projectOf("member-1").quizLevel shouldBe QuizLevel.L2
    }

    @Test
    fun `문제를 낼 수 없다고 판정된 저장소면 프로젝트를 만들지 않고 거절 사유를 그대로 알린다`() {
        quizRepoRepository.save(quizRepoOf().rejected(ErrorCode.NOT_FOUND))

        val exception = shouldThrow<BaseException> { registerProject(commandOf(memberId = "member-1", quizLevel = QuizLevel.L2)) }

        exception.errorCode shouldBe ErrorCode.NOT_FOUND
        projectRepository.count() shouldBe 0
    }

    private fun projectOf(memberId: String) = projectRepository.findAll().singleOrNull { it.memberId == memberId }.shouldNotBeNull()

    private fun quizRepoOf() =
        QuizRepo(
            githubRepoId = REPO.id,
            githubRepoUrl = REPO_URL,
            name = REPO.name,
            ownerImageUrl = REPO.ownerImageUrl,
            starCount = REPO.starCount,
            techStacks = REPO.techStacks,
            registeredAt = Instant.EPOCH,
        )

    private fun commandOf(
        memberId: String,
        quizLevel: QuizLevel,
    ) = RegisterProject.Command(
        memberId = memberId,
        githubRepoUrl = REPO_URL,
        quizLevel = quizLevel,
    )

    companion object {
        private const val REPO_URL = "https://github.com/spring-projects/spring-petclinic"
        private const val REPO_ID = "7517918"

        private val REPO =
            GithubRepository(
                id = REPO_ID,
                name = "spring-petclinic",
                ownerImageUrl = "https://avatars.githubusercontent.com/u/317776?v=4",
                starCount = 7800,
                techStacks = listOf("java", "spring-boot", "petclinic"),
            )
    }
}
