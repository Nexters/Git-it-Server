package com.nexters.gitit.application.project

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.project.Answer
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
import com.nexters.gitit.domain.quizrepo.completed
import com.nexters.gitit.infrastructure.mongo.SpringDataProjectRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Instant

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class GetLearningSetTest(
    @Autowired private val getLearningSet: GetLearningSet,
    @Autowired private val projectRepository: SpringDataProjectRepository,
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
) {
    private lateinit var projectId: String

    @BeforeEach
    fun setUp() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        projectRepository.deleteAll()
        quizRepoRepository.deleteAll()

        val quizRepo = quizRepoRepository.save(quizRepoOf())
        projectId = projectRepository.save(Project(MEMBER_ID, quizRepo.id, Depth.L2)).id
    }

    @Test
    fun `프로젝트에 걸린 레벨의 문제만 저장 순서대로 돌려주고 출처에 GitHub 링크를 붙인다`() {
        val result = getLearningSet(GetLearningSet.Command(MEMBER_ID, projectId, SET_ID))

        result.title shouldBe "라우팅 흐름 따라가기"
        result.level shouldBe Depth.L2
        result.questions.map { it.question.id } shouldBe listOf(FIRST_QUESTION_ID, SECOND_QUESTION_ID)

        val source =
            result.questions
                .first()
                .sources
                .single()
        source.summary shouldBe "경로를 정의하는 자리"
        source.url shouldBe "https://github.com/Nexters/Git-it-Server/blob/abc1234/src/Router.kt#L10-L20"
    }

    @Test
    fun `이미 푼 문제에만 내가 낸 답이 붙는다`() {
        val project = projectRepository.findById(projectId).orElse(null).shouldNotBeNull()
        project.submit(Answer.Choice(FIRST_QUESTION_ID, Instant.now(), selectedIndex = 2, correct = true))
        projectRepository.save(project)

        val result = getLearningSet(GetLearningSet.Command(MEMBER_ID, projectId, SET_ID))

        val answer =
            result.questions
                .first()
                .answer
                .shouldBeInstanceOf<Answer.Choice>()
        answer.selectedIndex shouldBe 2
        answer.correct shouldBe true
        result.questions.last().answer shouldBe null
    }

    private fun quizRepoOf() =
        QuizRepo(
            githubRepoId = "1310710749",
            githubRepoUrl = "https://github.com/Nexters/Git-it-Server",
            name = "Git-it-Server",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
            registeredAt = Instant.EPOCH,
        ).completed("abc1234", listOf(learningSetOf()))

    private fun learningSetOf(): LearningSet {
        val anchor = Anchor("src/Router.kt", 10, 20, AnchorKind.DEFINITION, "class Router")
        return LearningSet(
            id = SET_ID,
            concept = Concept("라우팅", "라우팅은 `Router.kt`가 전담합니다.", "README.md", listOf("src/Router.kt")),
            title = "라우팅 흐름 따라가기",
            description = "요청이 어느 경로로 흘러가는지 확인하는 학습 세트입니다.",
            orientation = "요청은 `Router`가 받습니다.",
            notes = listOf(AnchorNote(anchor, "경로를 정의하는 자리")),
            // 고른 레벨만 나오는지 보려면 다른 레벨에도 문제가 있어야 한다.
            questions =
                mapOf(
                    Depth.L1 to listOf(questionOf("question-l1", Depth.L1, anchor)),
                    Depth.L2 to listOf(questionOf(FIRST_QUESTION_ID, Depth.L2, anchor), questionOf(SECOND_QUESTION_ID, Depth.L2, anchor)),
                    Depth.L3 to listOf(questionOf("question-l3", Depth.L3, anchor)),
                ),
        )
    }

    private fun questionOf(
        id: String,
        depth: Depth,
        anchor: Anchor,
    ) = Question(
        id = id,
        depth = depth,
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
        private const val SET_ID = "set-1"
        private const val FIRST_QUESTION_ID = "question-l2-1"
        private const val SECOND_QUESTION_ID = "question-l2-2"
    }
}
