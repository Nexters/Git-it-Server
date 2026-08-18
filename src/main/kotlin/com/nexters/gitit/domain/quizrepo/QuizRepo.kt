package com.nexters.gitit.domain.quizrepo

import com.nexters.gitit.domain.common.BaseEntity
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * GitHub 저장소 하나에서 뽑아낸 문제를 모아두는 곳.
 *
 * 회원이 아니라 저장소가 주인공인 공용 애그리거트라, 같은 저장소를 여러 회원이 등록해도 문제 세트는 하나만
 * 만들고 나눠 씁니다. 누가 이걸 학습하는지와 회원별 난이도는 `Project`가 들고 있어, 회원이 늘어도
 * 이 도큐먼트는 커지지 않습니다.
 *
 * 산출물을 별도 컬렉션으로 빼지 않고 [learningSets]로 품는 이유는 언제나 함께 읽히기 때문입니다 —
 * "이 저장소의 문제"를 보는 데 조회가 두 번 필요할 이유가 없습니다. 크기는 회원 수가 아니라 개념 수에
 * 비례하고, 개념은 레포당 몇 개 수준이라 도큐먼트 상한(16MB)에서 멀리 떨어져 있습니다.
 *
 * [sha]는 [learningSets]와 [anchoredConcepts]가 어느 커밋을 보고 만들어졌는지입니다. 앵커가 라인 번호라
 * 커밋이 달라지면 같은 파일이라도 다른 곳을 가리켜, 레포가 갱신됐을 때 옛 산출물을 그대로 쓰면 안 됩니다.
 * 수집 단계가 확정해 [RepoCheckout]에 실어 보낸 값을 그대로 받습니다 — 해제 디렉터리 이름에서 되짚지 않습니다.
 *
 * [name]·[ownerImageUrl]·[starCount]·[techStacks]는 목록에 그려주려고 들고 있는 첫 등록 시점의 스냅샷입니다.
 * 갱신하지 않는 것은 저장소당 하나뿐인 공용 도큐먼트라 다시 읽어올 시점을 정해줄 주인이 없어서입니다.
 */
@Document(collection = "quiz_repos")
@CompoundIndex(
    name = "uk_github_repo_id",
    def = "{'githubRepoId': 1}",
    unique = true,
    partialFilter = "{'deletedAt': null}",
)
// 스케줄러가 몇 초마다 이 순서로 대기줄을 훑는다. 정렬까지 인덱스가 받아 줘야 매번 컬렉션을 훑고 메모리에서 정렬하지 않는다.
// 이름만 옛 용어(pending)로 남아 있다. 키가 같고 이름만 다른 인덱스는 만들 수 없어, 바꾸려면 운영 DB에서 먼저 떨어뜨려야 기동된다.
@CompoundIndex(
    name = "idx_pending",
    def = "{'status': 1, 'registeredAt': 1}",
    partialFilter = "{'deletedAt': null}",
)
class QuizRepo(
    val githubRepoId: String,
    val githubRepoUrl: String,
    val name: String,
    val ownerImageUrl: String,
    val starCount: Int,
    val techStacks: List<String>,
    registeredAt: Instant,
) : BaseEntity() {
    // 생성 파이프라인이 최종 상태를 결정하므로 등록 시점에는 항상 시작 상태다.
    var status: QuizRepoStatus = QuizRepoStatus.READY
        private set

    /**
     * 생성 대기줄에 선 시각. 등록과 [retry]가 각각 갱신합니다.
     *
     * `createdAt`으로 줄을 세우지 않는 것은 재시도가 줄에 다시 서는 행위이기 때문입니다 — 몇 주 전에
     * 등록된 저장소가 재시도할 때마다 맨 앞으로 끼어들면 방금 등록한 사람이 계속 밀립니다.
     */
    var registeredAt: Instant = registeredAt
        private set

    // 전용 enum을 만들지 않고 ErrorCode를 재사용하는 것은, 어차피 클라이언트에게 같은 코드로 알려줘야 해서 목록이 두 벌이 되기 때문이다.
    var rejectedReason: ErrorCode? = null
        private set

    /**
     * 점유가 언제까지 유효한지. 쥐고 있는 실행이 없으면 null입니다.
     *
     * [QuizRepoStatus.STARTED]와 같은 말이라 [start]가 채우고 [finish]·[retry]가 함께 비웁니다 —
     * 끝난 저장소에 값이 남으면 죽은 점유가 아직 유효해 보입니다.
     */
    var timeoutAt: Instant? = null
        private set

    var sha: String? = null
        private set

    // 앵커 단계를 지나면 채워진다. 재시도 때 아껴 쓸 재료라 결말이 나도 지우지 않는다.
    var anchoredConcepts: List<AnchoredConcept> = emptyList()
        private set

    // COMPLETED에서만 채워진다.
    var learningSets: List<LearningSet> = emptyList()
        private set

    /**
     * 생성을 점유합니다. 대기 중이 아니거나 이미 남이 쥐고 있으면 false이고, 이때 이 객체는 그대로입니다.
     *
     * true를 받았다면 [starter]가 저장까지 마친 뒤이므로 따로 저장하지 않아도 됩니다.
     */
    fun start(
        starter: QuizGenerationStarter,
        now: Instant,
        timeout: Duration,
    ): Boolean {
        val deadline = now.plus(timeout)
        if (!starter.start(id, deadline)) return false

        status = QuizRepoStatus.STARTED
        timeoutAt = deadline
        return true
    }

    /**
     * 문제까지는 못 갔지만 확정된 개념·앵커를 붙잡아 둡니다. 완성본이 아니라 **다시 만들 때 아껴 쓸 재료**입니다.
     *
     * 결말이 아니라 진행 중 저장이라 상태도 [timeoutAt]도 건드리지 않습니다 — 점유는 그대로입니다.
     *
     * [sha]를 함께 받는 이유는 [AnchoredConcept]의 앵커가 라인 번호이기 때문입니다. 재료가 어느 커밋에서
     * 나왔는지 같이 적혀 있지 않으면, 갱신된 레포에서 그대로 쓸 수 있는지를 판정할 방법이 없습니다.
     */
    fun checkpoint(
        sha: String,
        anchored: List<AnchoredConcept>,
    ) {
        this.sha = sha
        anchoredConcepts = anchored
    }

    /** 상태와 산출물을 함께 바꿔, 완료라면서 문제가 없는 저장소가 생기지 않게 합니다. */
    fun complete(
        now: Instant,
        sha: String,
        sets: List<LearningSet>,
    ) = finish(now) {
        this.sha = sha
        learningSets = sets
        status = QuizRepoStatus.COMPLETED
    }

    /** 상태와 사유를 함께 바꿔 사유 없는 [QuizRepoStatus.REJECTED]가 생기지 않게 합니다. */
    fun reject(
        now: Instant,
        reason: ErrorCode,
    ) = finish(now) {
        status = QuizRepoStatus.REJECTED
        rejectedReason = reason
    }

    /** [reject]와 달리 판정이 아니라 사고라 사유를 받지 않습니다. 왜 죽었는지는 로그에서 봅니다. */
    fun fail(now: Instant) =
        finish(now) {
            status = QuizRepoStatus.FAILED
        }

    /**
     * 지난 회차가 만들어 둔 앵커. 없거나 [sha]가 다르면 빈 리스트입니다.
     *
     * 같은 커밋일 때로 한정하는 것은 앵커가 라인 번호이기 때문입니다. 갱신된 레포에 옛 앵커를 쓰면 이미
     * 게이트를 통과한 값이라 뒷단계 검증에도 걸리지 않은 채 엉뚱한 코드를 인용합니다.
     */
    fun cachedAnchors(sha: String): List<AnchoredConcept> = if (this.sha == sha) anchoredConcepts else emptyList()

    /**
     * 점유가 아직 유효할 때만 [outcome]을 적고 점유를 놓습니다. 시효가 지났으면
     * [ErrorCode.QUIZ_GENERATION_TIMED_OUT]을 던져 결과를 버립니다 — 멎었다 깨어난 실행이 그 사이 새로
     * 시작된 회차의 결과를 덮지 않게 하는 자리입니다. 결말을 늘릴 때도 이 자리를 지나가게 합니다.
     */
    private fun finish(
        now: Instant,
        outcome: () -> Unit,
    ) {
        if (timeoutAt?.isAfter(now) != true) {
            throw BaseException(ErrorCode.QUIZ_GENERATION_TIMED_OUT)
        }

        outcome()
        timeoutAt = null
    }

    /**
     * 사고로 멈춘 저장소를 대기줄 맨 뒤에 다시 세웁니다. [QuizRepoStatus.FAILED]가 아니면
     * [ErrorCode.QUIZ_GENERATION_NOT_RETRYABLE]을 던집니다.
     *
     * 실패 직전 상태가 무엇이었든 언제나 READY로 돌아갑니다 — 대기줄이 READY 하나로 정의되어,
     * 다른 값으로 되돌리면 아무도 집어 가지 않습니다. 앵커를 다시 만들지 않는 근거는 [anchoredConcepts]가 맡습니다.
     */
    fun retry(clock: Clock) {
        if (status != QuizRepoStatus.FAILED) {
            throw BaseException(ErrorCode.QUIZ_GENERATION_NOT_RETRYABLE)
        }

        status = QuizRepoStatus.READY
        registeredAt = Instant.now(clock)
        timeoutAt = null
    }

    /**
     * 임베드된 문제를 id로 찾습니다. 어느 학습 세트에도 그 id가 없으면 null입니다.
     *
     * 문제가 세트 안에서 레벨로 다시 중첩돼 있어 id 하나로 집으려면 전부 훑는 수밖에 없습니다.
     * 세트는 레포당 몇 개, 문제는 레벨당 몇 개 수준이라 순회 비용은 문제가 되지 않습니다.
     */
    fun findQuestion(questionId: String): Question? = learningSets.flatMap { it.questions.values.flatten() }.find { it.id == questionId }

    fun findLearningSet(setId: String): LearningSet? = learningSets.find { it.id == setId }

    /**
     * 앵커가 가리키는 코드를 GitHub에서 여는 주소.
     *
     * 브랜치가 아니라 [sha]로 고정합니다. 앵커가 라인 번호라, 브랜치 링크는 레포가 갱신되는 순간
     * 문제가 인용한 곳과 다른 코드를 열어줍니다.
     *
     * [githubRepoUrl]은 사용자가 적어 넣은 문자열 그대로라 접미사가 제각각입니다. 등록 때 쓰는 정규식을
     * 여기서 다시 쓰지 않는 것은, 그 검사는 "받아들일지 말지"를 정하는 것이고 여기 온 URL은 이미 통과한 값이기 때문입니다.
     */
    fun sourceUrlOf(anchor: Anchor): String {
        val base =
            githubRepoUrl
                .trim()
                .removeSuffix("/")
                .removeSuffix(".git")
                .let { if (it.startsWith("http")) it else "https://$it" }
        val lines = if (anchor.startLine == anchor.endLine) "L${anchor.startLine}" else "L${anchor.startLine}-L${anchor.endLine}"
        // 경로에 공백이나 #이 들어 있으면 붙이는 순간 주소가 끊긴다(#부터는 프래그먼트로 읽힌다).
        // 구분자 /는 그대로 두고 나머지만 인코딩해야 해서 URLEncoder가 아니라 URI에 맡긴다.
        val path = URI(null, null, anchor.file, null).rawPath

        // finish()가 sha와 세트를 함께 세팅하므로 문제가 있는데 sha가 없을 수는 없지만, 타입이 nullable이라 막아 둔다.
        return "$base/blob/${sha ?: error("문제는 있는데 sha가 없습니다: quizRepoId=$id")}/$path#$lines"
    }
}
