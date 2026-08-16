package com.nexters.gitit.domain.project

import com.nexters.gitit.domain.common.BaseEntity
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * 회원 한 명이 문제 저장소 하나를 학습하는 단위. 사용자에게 보이는 "내 프로젝트"가 이것입니다.
 *
 * 문제는 `QuizRepo`에 공용으로 모아두고 여기서는 그 참조와 회원별 난이도만 갖습니다. 회원이나 저장소 어느
 * 한쪽에 배열로 품지 않고 따로 둔 이유는 조회 방향이 둘 다 필요해서입니다 — "내 프로젝트 목록"은 [memberId]로
 * 찾고, 문제 생성이 끝났을 때 알릴 대상은 [quizRepoId]로 찾습니다. 두 방향 모두 인덱스로 풀리게 걸어 둡니다.
 */
@Document(collection = "projects")
@CompoundIndex(
    name = "uk_member_quiz_repo",
    def = "{'memberId': 1, 'quizRepoId': 1}",
    unique = true,
    partialFilter = "{'deletedAt': null}",
)
class Project(
    val memberId: String,
    @Indexed(name = "idx_quiz_repo_id")
    val quizRepoId: String,
    val quizLevel: QuizLevel,
) : BaseEntity() {
    // 진도는 따로 세지 않고 이 목록에서 파생합니다. 두 벌로 두면 답변과 진도가 서로 어긋납니다.
    var answers: List<Answer> = emptyList()
        private set

    /**
     * 답을 남기되 같은 문제에 대한 이전 답은 지웁니다. 복습이 기록을 쌓는 일이 아니라 최신 상태를 갱신하는
     * 일이라, 한 문제에 답이 둘 이상 남으면 "지금 이 문제를 맞히는가"에 답할 수 없습니다.
     */
    fun submit(answer: Answer) {
        answers = answers.filterNot { it.questionId == answer.questionId } + answer
    }

    /**
     * 주인이 아니면 [BaseException]을 던집니다. 권한 없음이 아니라 없는 것으로 답하는 이유는,
     * 403이 곧 "그 id의 프로젝트는 존재한다"를 알려주는 셈이기 때문입니다.
     */
    fun requireOwnedBy(memberId: String) {
        if (this.memberId != memberId) {
            throw BaseException(ErrorCode.PROJECT_NOT_FOUND)
        }
    }
}
