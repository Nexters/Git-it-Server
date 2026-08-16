package com.nexters.gitit.infrastructure.quiz

import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.LearningSet
import com.nexters.gitit.domain.quizrepo.QualityTag
import com.nexters.gitit.domain.quizrepo.Question
import com.nexters.gitit.domain.quizrepo.QuestionFormat
import com.nexters.gitit.infrastructure.repo.SourceBundler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

private val logger = KotlinLogging.logger {}

/**
 * 생성된 산문과 선택지를 코드와 대조해 의심 구간에 표시를 남깁니다. LLM 콜을 쓰지 않습니다.
 *
 * 게이트가 아닙니다 — 아무것도 버리지 않고 태그만 붙입니다.
 * 여기서 걸리는 것은 "거짓이다"가 아니라 "의심스럽다"이고, 무엇을 감출지는 서빙이 정합니다.
 *
 * Phase 1에는 오답 후보 주입도 블라인드 검증도 없어 **오답 품질을 보는 장치가 여기 하나뿐**입니다.
 */
@Component
class QualityInspector(
    private val sourceBundler: SourceBundler,
) {
    fun inspect(
        repoRoot: Path,
        sets: List<LearningSet>,
    ): List<LearningSet> = sets.map { inspect(repoRoot, it) }

    private fun inspect(
        repoRoot: Path,
        set: LearningSet,
    ): LearningSet {
        val haystack = haystack(repoRoot, set.notes.map { it.anchor })
        val prose = (listOf(set.orientation) + set.notes.map { it.summary }).joinToString("\n")

        return set.copy(
            tags = set.tags + suspectOf(prose, haystack, QualityTag.PROSE_SUSPECT, set.concept.name, "prose"),
            questions = set.questions.mapValues { (_, questions) -> questions.map { inspect(it, haystack, set.concept.name) } },
        )
    }

    private fun inspect(
        question: Question,
        haystack: String,
        conceptName: String,
    ): Question {
        val distractors =
            if (question.format == QuestionFormat.MULTIPLE_CHOICE) {
                suspectOf(question.choices.joinToString("\n"), haystack, QualityTag.DISTRACTOR_SUSPECT, conceptName, "choices")
            } else {
                emptySet()
            }

        // 앵커 인용 수는 형태일 뿐 난이도가 아니지만, L3인데 앵커가 하나면 연결할 것이 애초에 없다.
        val depth =
            if (question.depth == Depth.L3 && question.anchors.size < MINIMUM_L3_ANCHORS) {
                setOf(QualityTag.DEPTH_SUSPECT)
            } else {
                emptySet()
            }

        return question.copy(tags = question.tags + distractors + depth)
    }

    private fun suspectOf(
        text: String,
        haystack: String,
        tag: QualityTag,
        conceptName: String,
        what: String,
    ): Set<QualityTag> {
        val unknown = identifiers(text).filterNot { it in haystack }
        if (unknown.isEmpty()) return emptySet()

        logger.debug { "Tagged $tag on '$conceptName' $what: $unknown" }
        return setOf(tag)
    }

    /**
     * 산문에서 코드 식별자만 추려냅니다.
     *
     * 백틱 코드스팬을 먼저 보는 이유는 `Router`처럼 낙타 등이 하나뿐인 이름 때문입니다 —
     * 표기만으로는 평범한 영어 단어와 구분되지 않아, 생성 프롬프트가 백틱을 요구하고 여기서 그것을 믿습니다.
     * 백틱 밖에서는 오탐이 적은 꼴(낙타 등 둘 이상·snake_case·호출 꼴)만 봅니다.
     */
    private fun identifiers(text: String): List<String> {
        val quoted = CODE_SPAN.findAll(text).flatMap { span -> TOKEN.findAll(span.groupValues[1]).map { it.value } }
        val bare = IDENTIFIER.findAll(text).map { it.value }

        // 한두 글자 토큰은 어느 파일에나 있어 대조가 의미 없다.
        return (quoted + bare).distinct().filter { it.length >= MINIMUM_IDENTIFIER_LENGTH }.toList()
    }

    /**
     * 대조 범위는 앵커 파일과 그 파일이 있는 디렉터리뿐입니다.
     *
     * 레포 전체로 넓히면 `User`·`save`·`Config` 같은 흔한 이름이 어디에나 있어 통과율이 100%가 됩니다.
     * 범위를 좁혀야 "레포 어딘가엔 있지만 이 맥락엔 없는 이름"이 걸립니다.
     */
    private fun haystack(
        repoRoot: Path,
        anchors: List<Anchor>,
    ): String {
        val directories = anchors.map { it.file.substringBeforeLast('/', "") }.distinct()
        val neighbours =
            directories
                .flatMap { siblings(repoRoot, it) }
                .sorted()
                .take(MAX_NEIGHBOUR_FILES)

        return (anchors.map { it.file } + neighbours)
            .distinct()
            .mapNotNull { sourceBundler.lines(repoRoot, it) }
            .joinToString("\n") { it.joinToString("\n") }
    }

    private fun siblings(
        repoRoot: Path,
        directory: String,
    ): List<String> =
        runCatching {
            val base = if (directory.isEmpty()) repoRoot else repoRoot.resolve(directory)
            val prefix = if (directory.isEmpty()) "" else "$directory/"
            base.listDirectoryEntries().filter { it.isRegularFile() }.map { prefix + it.fileName }
        }.getOrDefault(emptyList())

    companion object {
        // 디렉터리 하나가 수백 파일일 수 있다. 대조 범위는 넓힐수록 검사가 무의미해지므로 캡이 정확도의 일부다.
        private const val MAX_NEIGHBOUR_FILES = 30
        private const val MINIMUM_L3_ANCHORS = 2
        private const val MINIMUM_IDENTIFIER_LENGTH = 3

        // 낙타 등 둘 이상 · snake_case · 호출 꼴. 한국어 산문과 평범한 영어 단어는 걸리지 않아 스톱워드 목록이 필요 없다.
        private val IDENTIFIER =
            Regex("""\b[A-Z][a-z0-9]+(?:[A-Z][a-z0-9]*)+\b|\b[a-z][a-z0-9]*(?:_[a-z0-9]+)+\b|\b[A-Za-z_]\w*(?=\()""")
        private val CODE_SPAN = Regex("`([^`]+)`")
        private val TOKEN = Regex("""[A-Za-z_]\w*""")
    }
}
