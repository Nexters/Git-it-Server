package com.nexters.gitit.application.project

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.AnchorKind
import com.nexters.gitit.domain.quizrepo.AnchorNote
import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.LearningSet
import com.nexters.gitit.domain.quizrepo.Question
import com.nexters.gitit.domain.quizrepo.QuestionFormat
import com.nexters.gitit.domain.quizrepo.QuestionType
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import com.nexters.gitit.domain.quizrepo.completed
import com.nexters.gitit.infrastructure.mongo.SpringDataProjectRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Instant

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class GetProjectsTest(
    @Autowired private val getProjects: GetProjects,
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
    fun `문제 생성이 끝난 저장소의 프로젝트를 진행 상태와 함께 돌려준다`() {
        val quizRepo = quizRepoRepository.save(completedQuizRepoOf("repo-1"))
        val project = projectRepository.save(Project(MEMBER_ID, quizRepo.id, Depth.L2))

        val result = getProjects(GetProjects.Command(MEMBER_ID))

        val item = result.items.single()
        item.projectId shouldBe project.id
        item.repositoryName shouldBe "Git-it-Server"
        item.currentSetLabel shouldBe "Set 1"
        item.nextQuestionId shouldBe QUESTION_ID
        item.overallProgressPercent shouldBe 0
    }

    @Test
    fun `문제 생성이 끝나지 않은 저장소의 프로젝트는 목록에서 뺀다`() {
        val completed = quizRepoRepository.save(completedQuizRepoOf("repo-1"))
        val ready = quizRepoRepository.save(quizRepoOf("repo-2"))
        ready.status shouldBe QuizRepoStatus.READY

        val project = projectRepository.save(Project(MEMBER_ID, completed.id, Depth.L2))
        projectRepository.save(Project(MEMBER_ID, ready.id, Depth.L2))

        val result = getProjects(GetProjects.Command(MEMBER_ID))

        result.items.map { it.projectId } shouldBe listOf(project.id)
    }

    private fun completedQuizRepoOf(githubRepoId: String) = quizRepoOf(githubRepoId).completed("abc1234", listOf(learningSetOf()))

    private fun quizRepoOf(githubRepoId: String) =
        QuizRepo(
            githubRepoId = githubRepoId,
            githubRepoUrl = "https://github.com/Nexters/Git-it-Server",
            name = "Git-it-Server",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
            registeredAt = Instant.EPOCH,
        )

    private fun learningSetOf(): LearningSet {
        val anchor = Anchor("src/Router.kt", 10, 20, AnchorKind.DEFINITION, "class Router")
        return LearningSet(
            id = "set-1",
            concept = Concept("라우팅", "라우팅은 `Router.kt`가 전담합니다.", "README.md", listOf("src/Router.kt")),
            title = "라우팅 흐름 따라가기",
            description = "요청이 어느 경로로 흘러가는지 확인하는 학습 세트입니다.",
            orientation = "요청은 `Router`가 받습니다.",
            notes = listOf(AnchorNote(anchor, "경로를 정의하는 자리")),
            questions = mapOf(Depth.L2 to listOf(questionOf(anchor))),
        )
    }

    private fun questionOf(anchor: Anchor) =
        Question(
            id = QUESTION_ID,
            depth = Depth.L2,
            type = QuestionType.STRUCTURE,
            format = QuestionFormat.MULTIPLE_CHOICE,
            text = "경로를 정의하는 파일은?",
            choices = listOf("App.kt", "Main.kt", "Router.kt", "Config.kt"),
            answerIndex = 2,
            explanation = "라우팅은 Router가 전담한다",
            hints = listOf("src 아래를 보라"),
            rubric = null,
            anchors = listOf(anchor),
        )

    companion object {
        private const val MEMBER_ID = "member-1"
        private const val QUESTION_ID = "question-l2-1"
    }
}
