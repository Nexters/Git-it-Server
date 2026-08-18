package com.nexters.gitit.application

import com.nexters.gitit.infrastructure.mongo.QuizGenerationReclaimer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * 시효를 넘긴 점유를 대기줄로 되돌립니다.
 *
 * 점유한 실행이 SIGKILL·OOM으로 죽으면 스스로 결말을 적지 못해, 그 저장소는 아무도 집어 가지 않는
 * 자리에 남습니다. [GenerateQuiz]와 짝을 이루는 유스케이스입니다 — 저쪽이 점유하고 이쪽이 놓아줍니다.
 */
@Service
class ReclaimQuizGeneration(
    private val quizGenerationReclaimer: QuizGenerationReclaimer,
    private val clock: Clock,
) {
    /**
     * 되돌린 개수를 로그로 남깁니다. 이 값이 꾸준히 올라오면 시효([GenerateQuiz] `TIMEOUT`)가
     * 실제 소요보다 짧다는 뜻입니다.
     */
    operator fun invoke() {
        val reclaimed = quizGenerationReclaimer.reclaim(Instant.now(clock))
        if (reclaimed > 0) {
            logger.warn { "Reclaimed $reclaimed expired quiz generation(s)" }
        }
    }
}
