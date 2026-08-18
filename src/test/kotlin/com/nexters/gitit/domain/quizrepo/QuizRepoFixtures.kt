package com.nexters.gitit.domain.quizrepo

import com.nexters.gitit.domain.exception.ErrorCode
import java.time.Duration
import java.time.Instant

/**
 * 결말은 점유를 쥔 실행만 적을 수 있습니다. 끝난 저장소가 그저 재료로 필요한 테스트는 그 규칙을 검증하지
 * 않으므로, 점유부터 결말까지를 여기 한 줄로 묶습니다.
 *
 * 점유 규칙 자체는 `QuizRepoTest`와 `MongoQuizGenerationStarterTest`가 봅니다.
 */
fun QuizRepo.started() = apply { start({ _, _ -> true }, STARTED_AT, LONG_ENOUGH) }

fun QuizRepo.completed(
    sha: String,
    sets: List<LearningSet>,
) = started().apply { complete(STARTED_AT, sha, sets) }

fun QuizRepo.rejected(reason: ErrorCode) = started().apply { reject(STARTED_AT, reason) }

fun QuizRepo.failed() = started().apply { fail(STARTED_AT) }

private val STARTED_AT = Instant.EPOCH

// 시효를 넉넉히 잡아, 결말을 적는 시각이 언제든 이 픽스처가 시효에 걸리지 않게 한다.
private val LONG_ENOUGH = Duration.ofDays(1)
