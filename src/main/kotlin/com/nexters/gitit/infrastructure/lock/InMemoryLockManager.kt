package com.nexters.gitit.infrastructure.lock

import com.nexters.gitit.domain.common.LockManager
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * JVM 안에서만 유효한 락. 단일 인스턴스 전제라 이것으로 충분합니다.
 *
 * 키마다 `ReentrantLock`을 만들지 않는 것은 필요한 것이 대기가 아니라 거절이고, 락 객체를 언제 지울지가
 * 곧바로 새 문제가 되기 때문입니다. 맵 항목 하나면 그 문제가 없습니다.
 *
 * 프로세스와 함께 사라지는 것이 여기서는 이점이기도 합니다. 재기동 뒤에 락이 없는데 도큐먼트가 실행 중이면
 * 그 작업은 앞선 프로세스와 함께 죽은 것입니다.
 */
@Component
class InMemoryLockManager(
    private val clock: Clock,
) : LockManager {
    private val held = ConcurrentHashMap<String, Holder>()

    override fun <T> hold(
        key: String,
        lease: Duration,
        waitFor: Duration,
        action: () -> T,
    ): T? {
        val holder = acquire(key, lease, waitFor) ?: return null

        return try {
            action()
        } finally {
            // 값까지 맞아야 지우는 remove다. lease가 지나 다른 요청이 이미 가져갔다면 그쪽 락은 건드리지 않는다.
            held.remove(key, holder)
        }
    }

    private fun acquire(
        key: String,
        lease: Duration,
        waitFor: Duration,
    ): Holder? {
        val deadline = Instant.now(clock).plus(waitFor)

        while (true) {
            tryAcquire(key, lease)?.let { return it }
            if (!Instant.now(clock).isBefore(deadline)) return null
            // 폴링으로 두는 것은 대기가 아직 쓰이는 자리가 없어서다. 경합이 잦아지면 키별 조건변수로 바꾼다.
            Thread.sleep(RETRY_INTERVAL)
        }
    }

    /**
     * `compute`는 키 하나에 대해 원자적이라, 비었는지 보는 것과 채우는 것이 갈라지지 않습니다.
     *
     * 만료된 항목을 따로 청소하지 않는 것은 여기서 덮어쓰기 때문입니다 — 다시 쓰이지 않는 키만 남는데,
     * 키가 애그리거트 id라 개수가 도큐먼트 수를 넘지 않습니다.
     */
    private fun tryAcquire(
        key: String,
        lease: Duration,
    ): Holder? {
        val now = Instant.now(clock)
        var acquired: Holder? = null

        held.compute(key) { _, current ->
            if (current != null && current.expiresAt.isAfter(now)) {
                current
            } else {
                Holder(UUID.randomUUID().toString(), now.plus(lease)).also { acquired = it }
            }
        }

        return acquired
    }

    /** 토큰이 있어야 만료로 주인이 바뀐 락을 옛 점유자가 풀어 버리는 일을 막습니다. */
    private data class Holder(
        val token: String,
        val expiresAt: Instant,
    )

    companion object {
        private val RETRY_INTERVAL = Duration.ofMillis(100)
    }
}
