package com.nexters.gitit.domain.quizrepo

import com.nexters.gitit.domain.common.BaseEntity
import com.nexters.gitit.domain.exception.ErrorCode
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

/**
 * GitHub 저장소 하나에서 뽑아낸 문제를 모아두는 곳.
 *
 * 회원이 아니라 저장소가 주인공인 공용 애그리거트라, 같은 저장소를 여러 회원이 등록해도 문제 세트는 하나만
 * 만들고 나눠 씁니다. 누가 이걸 학습하는지와 회원별 난이도는 `Project`가 들고 있어, 회원 수와 무관하게
 * 이 도큐먼트의 크기가 고정됩니다.
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

    /**
     * 상태와 사유를 함께 바꿔 사유 없는 [QuizRepoStatus.REJECTED]가 생기지 않게 합니다.
     */
    fun reject(reason: ErrorCode) {
        status = QuizRepoStatus.REJECTED
        rejectedReason = reason
    }
}
