package com.nexters.gitit.infrastructure.quiz

import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * LLM 콜을 동시에 쏘고 결과를 **입력 순서 그대로** 모아 돌려줍니다.
 * 응답이 돌아온 순서로 모으면 같은 레포를 두 번 돌렸을 때 산출물 순서가 달라집니다.
 *
 * [block]이 던진 예외는 감싸지 않고 그대로 올라갑니다.
 */
fun <T, R> List<T>.inParallel(block: (T) -> R): List<R> =
    Executors.newFixedThreadPool(MAX_CONCURRENT_CALLS).use { executor ->
        map { executor.submit<R> { block(it) } }.map { it.unwrapped() }
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
// 고정 풀은 스레드 재사용이 아니라 이 값을 강제하려고 쓴다 — 가상 스레드로 바꾸면 묶을 자리가 없어진다.
private const val MAX_CONCURRENT_CALLS = 3
