package com.nexters.gitit.infrastructure.lock

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class InMemoryLockManagerTest {
    @Test
    fun `점유 중인 키는 거절하고, 다른 키는 막지 않는다`() {
        val lockManager = InMemoryLockManager(Clock.systemUTC())
        var innerRan = false

        val result =
            lockManager.hold("a", LEASE) {
                lockManager.hold("a", LEASE) { innerRan = true } shouldBe null
                lockManager.hold("b", LEASE) { "b도 잡힌다" } shouldBe "b도 잡힌다"
                "a를 잡았다"
            }

        result shouldBe "a를 잡았다"
        innerRan shouldBe false
    }

    @Test
    fun `작업이 예외로 끝나도 락은 풀린다`() {
        val lockManager = InMemoryLockManager(Clock.systemUTC())

        shouldThrow<IllegalStateException> {
            lockManager.hold("a", LEASE) { error("작업이 터졌다") }
        }

        lockManager.hold("a", LEASE) { "다시 잡힌다" } shouldBe "다시 잡힌다"
    }

    /**
     * 프로세스가 죽어 finally를 못 도는 경우를 대신합니다 — 만료가 유일한 탈출구인 자리라,
     * 시간을 넘겨보지 않으면 그 길이 실제로 열려 있는지 확인할 방법이 없습니다.
     */
    @Test
    fun `lease가 지나면 아직 점유 중이어도 다음 요청이 가져간다`() {
        val clock = MutableClock(NOW)
        val lockManager = InMemoryLockManager(clock)

        val result =
            lockManager.hold("a", LEASE) {
                clock.now = NOW.plus(LEASE).plusSeconds(1)
                lockManager.hold("a", LEASE) { "만료된 락을 가져간다" }
            }

        result shouldBe "만료된 락을 가져간다"
    }

    /**
     * 만료로 주인이 바뀐 뒤 옛 점유자가 뒤늦게 끝나는 순간을 재현합니다. 토큰 대조가 없으면 여기서
     * 남의 락을 풀어 버려, 만료 자체보다 더 나쁜 겹침(둘 다 자기가 점유자라고 믿는)이 만들어집니다.
     */
    @Test
    fun `만료된 옛 점유자가 끝나도 새 점유자의 락은 풀리지 않는다`() {
        val clock = MutableClock(NOW)
        val lockManager = InMemoryLockManager(clock)
        val oldAcquired = CountDownLatch(1)
        val newAcquired = CountDownLatch(1)
        val oldFinished = CountDownLatch(1)
        var stillHeld = false

        val oldHolder =
            thread {
                lockManager.hold("a", LEASE) {
                    oldAcquired.countDown()
                    newAcquired.await(1, TimeUnit.SECONDS)
                }
                oldFinished.countDown()
            }
        oldAcquired.await(1, TimeUnit.SECONDS)
        clock.now = NOW.plus(LEASE).plusSeconds(1)

        val newHolder =
            thread {
                lockManager.hold("a", LEASE) {
                    newAcquired.countDown()
                    oldFinished.await(1, TimeUnit.SECONDS)
                    stillHeld = lockManager.hold("a", LEASE) { } == null
                }
            }

        oldHolder.join()
        newHolder.join()
        stillHeld shouldBe true
    }

    @Test
    fun `waitFor를 준 쪽은 앞선 점유가 끝나기를 기다렸다가 잡는다`() {
        val lockManager = InMemoryLockManager(Clock.systemUTC())
        val acquired = CountDownLatch(1)
        val release = CountDownLatch(1)

        val holder =
            thread {
                lockManager.hold("a", LEASE) {
                    acquired.countDown()
                    release.await(1, TimeUnit.SECONDS)
                }
            }
        acquired.await(1, TimeUnit.SECONDS)

        // 스레드 안에서 단언하면 실패해도 테스트가 통과하므로, 결과만 들고 나와 본 스레드에서 본다.
        var waited: String? = null
        val waiter = thread { waited = lockManager.hold("a", LEASE, waitFor = Duration.ofSeconds(5)) { "기다렸다 잡았다" } }
        release.countDown()

        holder.join()
        waiter.join()
        waited shouldBe "기다렸다 잡았다"
    }

    /** 만료 판정을 보려면 시간을 앞으로 밀어야 하는데, `Clock.fixed`는 멈춰 있고 시스템 시계는 30분을 못 기다린다. */
    private class MutableClock(
        var now: Instant,
    ) : Clock() {
        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneId.of("UTC")

        override fun withZone(zone: ZoneId): Clock = this
    }

    companion object {
        private val NOW = Instant.parse("2026-08-17T00:00:00Z")
        private val LEASE = Duration.ofMinutes(30)
    }
}
