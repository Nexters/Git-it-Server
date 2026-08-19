package com.nexters.gitit.infrastructure.quiz

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

private val logger = KotlinLogging.logger {}

/**
 * LLM 콜을 동시에 쏘고 결과를 **입력 순서 그대로** 모아 돌려줍니다.
 * 응답이 돌아온 순서로 모으면 같은 레포를 두 번 돌렸을 때 산출물 순서가 달라집니다.
 *
 * [block]이 던진 예외는 감싸지 않고 그대로 올라갑니다. 실패를 어떻게 다룰지는 부르는 쪽이 정합니다 —
 * 개념 하나가 죽어도 나머지를 살리려면 [block] 안을 `runCatching`으로 감싸고 [successesOrThrow]로 받습니다.
 */
fun <T, R> List<T>.inParallel(block: (T) -> R): List<R> =
    Executors.newFixedThreadPool(MAX_CONCURRENT_CALLS).use { executor ->
        map { executor.submit<R> { block(it) } }.map { it.unwrapped() }
    }

/**
 * 성공한 것만 골라내되, **전부 실패했으면 첫 예외를 그대로 던집니다.** 버린 실패는 로그에 남습니다.
 *
 * 전부 실패는 개념의 문제가 아니라 사고(쿼터 소진·네트워크 단절)입니다. 삼키면 뒷단계가 빈 결과를
 * "재료가 없다"로 읽어 되돌릴 수 없는 거절로 굳습니다.
 *
 * [Error]와 인터럽트는 하나만 나와도 올립니다. `runCatching`이 [Throwable]을 잡으므로, 세지 않으면
 * JVM이 죽어가는 중에도 남은 결과로 세트가 완성돼 나갑니다.
 */
fun <T : Any> List<Result<T>>.successesOrThrow(): List<T> {
    val failures = mapNotNull { it.exceptionOrNull() }

    failures.firstOrNull { it is Error || it is InterruptedException }?.let { throw it }

    if (isNotEmpty() && failures.size == size) {
        throw failures.first()
    }

    // 버린 것을 남기지 않으면 산출물에서 개념이 왜 빠졌는지 되짚을 방법이 없다.
    failures.forEach { logger.warn(it) { "Skipped a failed call" } }

    return mapNotNull { it.getOrNull() }
}

/**
 * [Future.get]이 실패 원인을 [ExecutionException]으로 감싸는데, 그대로 두면 게이트가 던진
 * `BaseException`이 껍데기에 가려집니다. 부르는 쪽은 그걸 판정이 아니라 사고로 읽어,
 * "문제를 못 만드는 레포"가 "서버가 죽은 레포"로 둔갑합니다.
 */
private fun <T> Future<T>.unwrapped(): T =
    try {
        get()
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }

// 쿼터가 분당이라 한꺼번에 쏘면 뒤쪽이 전부 백오프 대기에 걸려, 올린 만큼 빨라지지 않는다.
// 고정 풀은 동시 콜 수를 이 값으로 묶어 두는 장치다.
private const val MAX_CONCURRENT_CALLS = 3
