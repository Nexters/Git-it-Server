package com.nexters.gitit.domain.common

import java.time.Duration

/**
 * 같은 키의 작업이 동시에 두 번 돌지 않게 막습니다.
 *
 * `lease`가 있는 것은 점유자가 죽거나 멎어도 락이 영원히 남지 않게 하기 위해서입니다. 프로세스를
 * 강제 종료하면 [hold]의 finally는 돌지 않으므로, 시간이 지나면 저절로 풀리는 길이 하나 있어야 합니다.
 *
 * `waitFor`의 기본값이 0인 것은 필요한 것이 대개 대기가 아니라 **거절**이어서입니다 — 몇 분짜리 작업을
 * 기다렸다가 똑같은 일을 한 번 더 하면 그게 막으려던 두 번째 실행입니다.
 *
 * 키는 호출부가 도메인 접두사를 붙여 만듭니다(`"quiz-generation:$id"`). 애그리거트가 달라도 식별자가
 * 겹칠 수 있어, id를 그대로 키로 쓰면 무관한 두 작업이 서로를 막습니다.
 */
interface LockManager {
    /**
     * 락을 점유한 채 [action]을 돌리고 반드시 놓습니다. [waitFor] 안에 점유하지 못하면 [action]을
     * 건너뛰고 null을 돌려줍니다.
     *
     * 못 잡은 것을 예외가 아니라 null로 알리는 이유는, 겹쳐 들어온 쪽이 할 일이 대개 로그 한 줄이라
     * 그때마다 try/catch를 쓰게 하고 싶지 않아서입니다. 대신 [action] 자체가 null을 돌려주는 경우와
     * 구분되지 않으니, 그런 [action]은 결과를 감싸서 넘깁니다.
     *
     * [lease]가 지나도 **돌던 스레드는 멈추지 않습니다.** 만료된 락을 다음 요청이 가져가면 그 순간만큼은
     * 둘이 겹칩니다. 스레드를 죽이는 대신 [lease]를 실측의 몇 배로 잡아 피합니다 — 진짜로 취소가
     * 필요해지면 그때 작업 자체에 취소 지점을 만듭니다.
     */
    fun <T> hold(
        key: String,
        lease: Duration,
        waitFor: Duration = Duration.ZERO,
        action: () -> T,
    ): T?
}
