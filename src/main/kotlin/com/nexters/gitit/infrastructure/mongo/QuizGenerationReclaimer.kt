package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class QuizGenerationReclaimer(
    private val mongoTemplate: MongoTemplate,
) {
    /**
     * [now] 기준으로 시효가 다한 점유를 전부 [QuizRepoStatus.READY]로 되돌리고, 되돌린 개수를 돌려줍니다.
     *
     * 산출물([QuizRepo.anchoredConcepts]·[QuizRepo.sha])과 [QuizRepo.registeredAt]은 그대로 둡니다 —
     * 다시 집힌 회차가 앞단을 건너뛰고, 기다린 만큼 앞줄에 섭니다.
     *
     * 시효 검사는 갱신 조건에 실려 있어야 합니다. 읽어서 판정한 뒤 쓰면 그 사이 새로 시작된 점유까지
     * 함께 풀어 멀쩡히 돌고 있는 회차를 끌어내립니다.
     */
    fun reclaim(now: Instant): Long {
        val query =
            Query(
                Criteria
                    .where("status")
                    .isEqualTo(QuizRepoStatus.STARTED)
                    .and("deletedAt")
                    .isEqualTo(null)
                    .and("timeoutAt")
                    .lte(now),
            )
        val update =
            Update()
                .set("status", QuizRepoStatus.READY)
                .unset("timeoutAt")

        return mongoTemplate.updateMulti(query, update, QuizRepo::class.java).modifiedCount
    }
}
