package com.nexters.gitit.domain.quizrepo

import com.nexters.gitit.domain.common.BaseEntity
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.net.URI
import java.time.Clock
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
     * 사고 직전에 어디까지 갔었는지. 사고가 난 적 없으면 null입니다.
     *
     * [retry]가 상태를 READY로 되돌린 뒤에도 남아, **대기 중이지만 앵커는 이미 있다**는 표식이 됩니다.
     * 대기줄이 READY 하나로 정의되는 이상 상태만으로는 갓 등록된 것과 구분할 방법이 없습니다.
     */
    var failedFrom: QuizRepoStatus? = null
        private set

    var sha: String? = null
        private set

    // ANCHORED부터 채워진다.
    var anchoredConcepts: List<AnchoredConcept> = emptyList()
        private set

    // COMPLETED에서만 채워진다.
    var learningSets: List<LearningSet> = emptyList()
        private set

    /**
     * 문제까지는 못 갔지만 개념·앵커는 확정된 상태로 만듭니다. 완성본이 아니라 **다시 만들 때 아껴 쓸 재료**라,
     * 여기서 멈춘 저장소는 학습자에게 내보낼 수 없습니다.
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
        status = QuizRepoStatus.ANCHORED
    }

    /** 상태와 산출물을 함께 바꿔, 완료라면서 문제가 없는 저장소가 생기지 않게 합니다. */
    fun complete(
        sha: String,
        sets: List<LearningSet>,
    ) {
        this.sha = sha
        learningSets = sets
        status = QuizRepoStatus.COMPLETED
        // 여기까지 왔으면 이어 쓸 재료가 아니라 결과다. 남겨 두면 완료된 저장소에 사고 흔적이 붙어 있게 된다.
        failedFrom = null
    }

    /**
     * 상태와 사유를 함께 바꿔 사유 없는 [QuizRepoStatus.REJECTED]가 생기지 않게 합니다.
     */
    fun reject(reason: ErrorCode) {
        status = QuizRepoStatus.REJECTED
        rejectedReason = reason
        failedFrom = null
    }

    /**
     * [reject]와 달리 여기 오는 것은 판정이 아니라 사고라 사유를 받지 않습니다.
     * 왜 죽었는지는 도큐먼트가 아니라 로그에서 봅니다 — 세거나 걸러야 할 값이 아닙니다.
     *
     * 어디까지 갔었는지만 [failedFrom]에 남깁니다. 재시도가 그 값을 보고 체크포인트를 이어 씁니다.
     */
    fun fail() {
        failedFrom = status
        status = QuizRepoStatus.FAILED
    }

    /**
     * 사고로 멈춘 저장소를 대기줄 맨 뒤에 다시 세웁니다. [QuizRepoStatus.FAILED]가 아니면
     * [ErrorCode.QUIZ_GENERATION_NOT_RETRYABLE]을 던집니다.
     *
     * [failedFrom]과 달리 상태는 언제나 READY로 돌아갑니다 — 대기줄이 READY 하나로 정의되어,
     * ANCHORED로 되돌리면 아무도 집어 가지 않습니다. 앵커를 다시 만들지 않는 근거는 [failedFrom]이 맡습니다.
     */
    fun retry(clock: Clock) {
        if (status != QuizRepoStatus.FAILED) {
            throw BaseException(ErrorCode.QUIZ_GENERATION_NOT_RETRYABLE)
        }

        status = QuizRepoStatus.READY
        registeredAt = Instant.now(clock)
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

        // complete()가 sha와 세트를 함께 세팅하므로 문제가 있는데 sha가 없을 수는 없지만, 타입이 nullable이라 막아 둔다.
        return "$base/blob/${sha ?: error("문제는 있는데 sha가 없습니다: quizRepoId=$id")}/$path#$lines"
    }
}
