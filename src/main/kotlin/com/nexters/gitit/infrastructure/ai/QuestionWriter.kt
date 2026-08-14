package com.nexters.gitit.infrastructure.ai

import com.nexters.gitit.domain.quizrepo.Concept
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.infrastructure.quiz.LearningSetDraft

/**
 * 개념 하나의 한 레벨을 쓰는 콜. 개념당 레벨 수만큼 호출됩니다.
 *
 * 컨텍스트는 그 개념의 앵커 본문과 문서 근거뿐입니다. 레포 전체나 다른 개념을 함께 넣으면
 * 토큰보다 개념 경계가 먼저 무너져, 어느 개념을 공부하는 세트인지 알 수 없게 됩니다.
 *
 * 세 레벨을 한 콜에 몰아 쓰게 하면 출력이 13,000토큰까지 불어 콜 하나가 3~6분 걸립니다.
 * 레벨로 쪼개면 같은 총량이 세 콜로 나뉘어 동시에 나갑니다.
 */
fun interface QuestionWriter {
    fun write(
        concept: Concept,
        anchorBundle: String,
        depth: Depth,
    ): LearningSetDraft
}
