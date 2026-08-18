package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.domain.quizrepo.QuizGenerationStarter
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 파생 쿼리로는 "지금 READY인 것만 STARTED로"를 한 번의 쓰기로 낼 수 없어, 이 프로젝트에서 유일하게
 * [MongoTemplate]을 직접 씁니다. 조건을 갱신 자체에 실어야 검사와 쓰기 사이에 남이 끼어들 틈이 없습니다.
 */
@Component
class MongoQuizGenerationStarter(
    private val mongoTemplate: MongoTemplate,
) : QuizGenerationStarter {
    override fun start(
        quizRepoId: String,
        timeoutAt: Instant,
    ): Boolean {
        val query =
            Query(
                Criteria
                    .where("id")
                    .isEqualTo(quizRepoId)
                    .and("deletedAt")
                    .isEqualTo(null)
                    .and("status")
                    .isEqualTo(QuizRepoStatus.READY),
            )
        val update =
            Update()
                .set("status", QuizRepoStatus.STARTED)
                .set("timeoutAt", timeoutAt)

        // updatedAt은 여기서 움직이지 않는다(@LastModifiedDate는 도큐먼트 전체 저장에만 붙는다). 뒤이은 결말 저장이 갱신한다.
        return mongoTemplate.updateFirst(query, update, QuizRepo::class.java).modifiedCount == 1L
    }
}
