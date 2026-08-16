package com.nexters.gitit.application

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.project.Answer
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.QuizLevel
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
import com.nexters.gitit.domain.quizrepo.Rubric
import com.nexters.gitit.domain.quizrepo.RubricCriterion
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

/**
 * 검증은 반환된 결과가 아니라 저장소에서 다시 읽은 프로젝트로 합니다. 답변이 객관식·서술형 두 타입으로
 * 나뉜 임베드 도큐먼트라, 되살릴 때 어느 타입인지를 매핑이 판별자 필드에 기대고 있습니다 —
 * 메모리에 있는 객체만 보면 그 왕복이 깨진 것을 잡지 못합니다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration::class)
class SubmitAnswerTest(
    @Autowired private val submitAnswer: SubmitAnswer,
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
        projectId = projectRepository.save(Project(MEMBER_ID, quizRepo.id, QuizLevel.L2)).id
    }

    @Test
    fun `객관식에 정답을 내면 정답 여부와 해설을 돌려주고 그 선택을 저장한다`() {
        val result = submitAnswer(choiceCommandOf(selectedIndex = ANSWER_INDEX))

        result.correct shouldBe true
        result.answerIndex shouldBe ANSWER_INDEX
        result.explanation shouldBe "라우팅은 Router가 전담한다"

        val answer = savedAnswers().single().shouldBeInstanceOf<Answer.Choice>()
        answer.questionId shouldBe CHOICE_QUESTION_ID
        answer.selectedIndex shouldBe ANSWER_INDEX
        answer.correct shouldBe true
        answer.answeredAt.shouldNotBeNull()
    }

    @Test
    fun `같은 문제를 다시 풀면 답이 쌓이지 않고 마지막 것만 남는다`() {
        submitAnswer(choiceCommandOf(selectedIndex = ANSWER_INDEX))

        val result = submitAnswer(choiceCommandOf(selectedIndex = 0))

        result.correct shouldBe false
        val answer = savedAnswers().single().shouldBeInstanceOf<Answer.Choice>()
        answer.selectedIndex shouldBe 0
        answer.correct shouldBe false
    }

    @Test
    fun `서술형은 채점하지 않고 낸 글을 그대로 저장하며 채점 기준을 돌려준다`() {
        val result =
            submitAnswer(
                SubmitAnswer.Command.Essay(
                    memberId = MEMBER_ID,
                    projectId = projectId,
                    questionId = ESSAY_QUESTION_ID,
                    text = "Router가 경로를 한 곳에서 관리해 흐름을 따라가기 쉬워집니다",
                ),
            )

        result.rubric.criteria
            .single()
            .points shouldBe 10

        val answer = savedAnswers().single().shouldBeInstanceOf<Answer.Essay>()
        answer.questionId shouldBe ESSAY_QUESTION_ID
        answer.text shouldBe "Router가 경로를 한 곳에서 관리해 흐름을 따라가기 쉬워집니다"
    }

    private fun savedAnswers(): List<Answer> =
        projectRepository
            .findById(projectId)
            .orElse(null)
            .shouldNotBeNull()
            .answers

    private fun choiceCommandOf(selectedIndex: Int) =
        SubmitAnswer.Command.Choice(
            memberId = MEMBER_ID,
            projectId = projectId,
            questionId = CHOICE_QUESTION_ID,
            selectedIndex = selectedIndex,
        )

    private fun quizRepoOf() =
        QuizRepo(
            githubRepoId = "1310710749",
            githubRepoUrl = "https://github.com/Nexters/Git-it-Server",
            name = "Git-it-Server",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 3,
            techStacks = listOf("Kotlin"),
        ).apply { complete("abc1234", listOf(learningSetOf())) }

    private fun learningSetOf(): LearningSet {
        val anchor = Anchor("src/Router.kt", 10, 20, AnchorKind.DEFINITION, "class Router")
        return LearningSet(
            id = "set-1",
            concept = Concept("라우팅", "라우팅은 `Router.kt`가 전담합니다.", "README.md", listOf("src/Router.kt")),
            title = "라우팅 흐름 따라가기",
            description = "요청이 어느 경로로 흘러가는지 확인하는 학습 세트입니다.",
            orientation = "요청은 `Router`가 받습니다.",
            notes = listOf(AnchorNote(anchor, "경로를 정의하는 자리")),
            questions =
                mapOf(
                    Depth.L1 to listOf(choiceQuestionOf(anchor)),
                    Depth.L3 to listOf(essayQuestionOf(anchor)),
                ),
        )
    }

    private fun choiceQuestionOf(anchor: Anchor) =
        Question(
            id = CHOICE_QUESTION_ID,
            depth = Depth.L1,
            type = QuestionType.STRUCTURE,
            format = QuestionFormat.MULTIPLE_CHOICE,
            text = "경로를 정의하는 파일은?",
            choices = listOf("App.kt", "Main.kt", "Router.kt", "Config.kt"),
            answerIndex = ANSWER_INDEX,
            explanation = "라우팅은 Router가 전담한다",
            hints = listOf("src 아래를 보라", "클래스 이름이 곧 역할이다"),
            rubric = null,
            anchors = listOf(anchor),
        )

    private fun essayQuestionOf(anchor: Anchor) =
        Question(
            id = ESSAY_QUESTION_ID,
            depth = Depth.L3,
            type = QuestionType.INTENT,
            format = QuestionFormat.ESSAY,
            text = "경로를 한곳에 모은 이유는?",
            choices = emptyList(),
            answerIndex = null,
            explanation = "흐름을 한 곳에서 따라갈 수 있다",
            hints = listOf("호출부가 흩어지면 어떻게 되는지 보라"),
            rubric =
                Rubric(
                    criteria = listOf(RubricCriterion("실제 파일명을 들어 설명했는가", 10)),
                    keyPoints = listOf("경로가 한곳에 모인다"),
                    fullMarkExample = "만점 답안",
                    partialExample = "부분 답안",
                    zeroExample = "0점 답안",
                ),
            anchors = listOf(anchor),
        )

    companion object {
        private const val MEMBER_ID = "member-1"
        private const val CHOICE_QUESTION_ID = "question-choice"
        private const val ESSAY_QUESTION_ID = "question-essay"
        private const val ANSWER_INDEX = 2
    }
}
