package com.nexters.gitit.application

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.QuizLevel
import com.nexters.gitit.domain.quizrepo.GithubRepositoryResolver
import com.nexters.gitit.domain.quizrepo.QuizGenerationRequested
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.infrastructure.mongo.SpringDataProjectRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@Import(TestcontainersConfiguration::class, RegisterProjectTest.QuizGenerationRequestedCaptor::class)
class RegisterProjectTest(
    @Autowired private val registerProject: RegisterProject,
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
    @Autowired private val projectRepository: SpringDataProjectRepository,
    @Autowired private val eventCaptor: QuizGenerationRequestedCaptor,
) {
    // 실제 구현은 GitHub API를 호출하므로 테스트에서 진짜로 돌릴 수 없다. MongoDB는 실제 구현을 쓴다.
    @MockitoBean
    private lateinit var githubRepositoryResolver: GithubRepositoryResolver

    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        quizRepoRepository.deleteAll()
        projectRepository.deleteAll()
        eventCaptor.clear()
        given(githubRepositoryResolver.resolve(REPO_URL)).willReturn(REPO_ID)
    }

    @Test
    fun `처음 등록하면 문제 저장소를 만들고 그 난이도로 프로젝트를 만들며 문제 생성을 요청한다`() {
        val result = registerProject(commandOf(memberId = "member-1", quizLevel = QuizLevel.L2))

        result.status shouldBe QuizRepoStatus.READY

        val quizRepo = quizRepoRepository.findByGithubRepoIdAndDeletedAtIsNull(REPO_ID).shouldNotBeNull()
        quizRepo.githubRepoUrl shouldBe REPO_URL

        val project = projectOf("member-1")
        project.id shouldBe result.projectId
        project.quizRepoId shouldBe quizRepo.id
        project.quizLevel shouldBe QuizLevel.L2

        eventCaptor.received shouldBe listOf(QuizGenerationRequested(quizRepo.id))
    }

    @Test
    fun `다른 회원이 같은 저장소를 등록하면 문제 저장소는 그대로 두고 프로젝트만 추가한다`() {
        val first = registerProject(commandOf(memberId = "member-1", quizLevel = QuizLevel.L2))
        eventCaptor.clear()

        val second = registerProject(commandOf(memberId = "member-2", quizLevel = QuizLevel.L3))

        second.projectId shouldNotBe first.projectId
        quizRepoRepository.count() shouldBe 1
        projectRepository.count() shouldBe 2
        // 이미 생성이 걸려 있으므로 중복 요청하지 않는다.
        eventCaptor.received.shouldBeEmpty()
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
        quizRepoRepository.save(QuizRepo(githubRepoId = REPO_ID, githubRepoUrl = REPO_URL).apply { reject(ErrorCode.NOT_FOUND) })

        val exception = shouldThrow<BaseException> { registerProject(commandOf(memberId = "member-1", quizLevel = QuizLevel.L2)) }

        exception.errorCode shouldBe ErrorCode.NOT_FOUND
        projectRepository.count() shouldBe 0
    }

    private fun projectOf(memberId: String) = projectRepository.findAll().singleOrNull { it.memberId == memberId }.shouldNotBeNull()

    private fun commandOf(
        memberId: String,
        quizLevel: QuizLevel,
    ) = RegisterProject.Command(
        memberId = memberId,
        githubRepoUrl = REPO_URL,
        quizLevel = quizLevel,
    )

    /**
     * 이벤트가 실제로 나갔는지만 봅니다. 퍼블리셔를 목으로 바꾸면 컨텍스트 전체의 이벤트가 죽어 다른 검증까지 흔들립니다.
     */
    class QuizGenerationRequestedCaptor {
        val received = mutableListOf<QuizGenerationRequested>()

        @EventListener
        fun capture(event: QuizGenerationRequested) {
            received += event
        }

        fun clear() = received.clear()
    }

    companion object {
        private const val REPO_URL = "https://github.com/spring-projects/spring-petclinic"
        private const val REPO_ID = "7517918"
    }
}
