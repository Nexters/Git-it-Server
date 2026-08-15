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
 *
 * 산출물이 이 도큐먼트 안에 임베드되므로, 개념·앵커·문제가 매핑을 거쳐 온전히 돌아오는지도 함께 봅니다 —
 * 중첩이 깊어 필드 하나가 조용히 누락돼도 저장은 성공합니다.
 */
@DataMongoTest
@Import(TestcontainersConfiguration::class, MongoAuditingConfiguration::class, ClockConfiguration::class)
class QuizRepoRepositoryTest(
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
) {
    private val anchor = Anchor("src/Router.kt", 10, 20, AnchorKind.DEFINITION, "class Router")

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

    @Test
    fun `문제를 채워 저장하면 개념·앵커·문제가 통째로 보존된다`() {
        val quizRepo = quizRepoOf(githubRepoUrl = "https://github.com/Nexters/Git-it-Server")
        quizRepo.complete(SHA, listOf(learningSet()))

        val saved = quizRepoRepository.save(quizRepo)
        val found = quizRepoRepository.findById(saved.id).orElse(null).shouldNotBeNull()

        found.status shouldBe QuizRepoStatus.COMPLETED
        found.sha shouldBe SHA
        found.createdAt.shouldNotBeNull()

        val set = found.learningSets.single()
        set.concept.rationale shouldBe "라우팅은 `Router.kt`가 전담합니다."
        set.tags shouldBe setOf(QualityTag.PROSE_SUSPECT)

        val question = set.questions.single()
        val rubric = question.rubric.shouldNotBeNull()
        question.anchors.single() shouldBe anchor
        rubric.criteria.single().points shouldBe 10
        question.tags shouldBe setOf(QualityTag.DEPTH_SUSPECT)
    }

    @Test
    fun `체크포인트를 남기면 앵커까지의 산출물과 커밋이 함께 보존된다`() {
        val quizRepo = quizRepoOf(githubRepoUrl = "https://github.com/Nexters/Git-it-Server")
        quizRepo.checkpoint(SHA, listOf(AnchoredConcept(concept(), listOf(anchor))))

        val saved = quizRepoRepository.save(quizRepo)
        val found = quizRepoRepository.findById(saved.id).orElse(null).shouldNotBeNull()

        // 재실행이 이어붙일지 판정하는 조건이 (상태, sha) 짝이라 둘을 함께 못 박는다.
        found.status shouldBe QuizRepoStatus.ANCHORED
        found.sha shouldBe SHA
        found.anchoredConcepts
            .single()
            .anchors
            .single() shouldBe anchor
    }

    private fun quizRepoOf(githubRepoUrl: String) = QuizRepo(githubRepoId = GITHUB_REPO_ID, githubRepoUrl = githubRepoUrl)

    private fun concept() = Concept("라우팅", "라우팅은 `Router.kt`가 전담합니다.", "README.md", listOf("src/Router.kt"))

    private fun learningSet() =
        LearningSet(
            concept = concept(),
            orientation = "요청은 `Router`가 받습니다.",
            notes = listOf(AnchorNote(anchor, "경로를 정의하는 자리")),
            questions = listOf(question()),
            tags = setOf(QualityTag.PROSE_SUSPECT),
        )

    private fun question() =
        Question(
            depth = Depth.L3,
            type = QuestionType.INTENT,
            format = QuestionFormat.ESSAY,
            text = "왜 이렇게 설계했는가",
            choices = emptyList(),
            answerIndex = null,
            explanation = "해설",
            hints = listOf("힌트 1", "힌트 2"),
            rubric =
                Rubric(
                    criteria = listOf(RubricCriterion("실제 파일명을 들었는가", 10)),
                    keyPoints = listOf("경로 확정은 코드가 한다"),
                    fullMarkExample = "만점 답안",
                    partialExample = "부분 답안",
                    zeroExample = "0점 답안",
                ),
            anchors = listOf(anchor),
            tags = setOf(QualityTag.DEPTH_SUSPECT),
        )

    companion object {
        private const val GITHUB_REPO_ID = "1310710749"
        private const val SHA = "abc1234"
    }
}
