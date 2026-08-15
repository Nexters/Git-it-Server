package com.nexters.gitit.domain.quizrepo

import com.nexters.gitit.domain.common.BaseEntity
import com.nexters.gitit.domain.exception.ErrorCode
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

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
 */
@Document(collection = "quiz_repos")
@CompoundIndex(
    name = "uk_github_repo_id",
    def = "{'githubRepoId': 1}",
    unique = true,
    partialFilter = "{'deletedAt': null}",
)
class QuizRepo(
    val githubRepoId: String,
    val githubRepoUrl: String,
) : BaseEntity() {
    // 생성 파이프라인이 최종 상태를 결정하므로 등록 시점에는 항상 시작 상태다.
    var status: QuizRepoStatus = QuizRepoStatus.READY
        private set

    // 전용 enum을 만들지 않고 ErrorCode를 재사용하는 것은, 어차피 클라이언트에게 같은 코드로 알려줘야 해서 목록이 두 벌이 되기 때문이다.
    var rejectedReason: ErrorCode? = null
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
    }

    /**
     * 상태와 사유를 함께 바꿔 사유 없는 [QuizRepoStatus.REJECTED]가 생기지 않게 합니다.
     */
    fun reject(reason: ErrorCode) {
        status = QuizRepoStatus.REJECTED
        rejectedReason = reason
    }

    /**
     * [reject]와 달리 여기 오는 것은 판정이 아니라 사고라 사유를 받지 않습니다.
     * 어디서 왜 죽었는지는 도큐먼트가 아니라 로그에서 봅니다 — 세거나 걸러야 할 값이 아닙니다.
     */
    fun fail() {
        status = QuizRepoStatus.FAILED
    }
}
