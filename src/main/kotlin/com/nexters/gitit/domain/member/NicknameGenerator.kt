package com.nexters.gitit.domain.member

import org.springframework.stereotype.Component
import kotlin.random.Random

/**
 * 회원 닉네임을 무작위로 만듭니다. 결과는 항상 15자 이하이고, 이미 쓰는 닉네임이 다시 나올 수 있습니다.
 *
 * 15자는 형용사(4자 이하)와 기술어(10자 이하) 목록으로만 지킵니다. 단어를 추가하다 상한을 넘기면
 * `NicknameGeneratorTest`가 모든 조합을 재서 걸러냅니다.
 */
@Component
class NicknameGenerator {
    internal val adjectives =
        """
        겁없는 야근하는 철야하는 지친 말많은 커밋하는 배포하는 롤백하는
        졸린 성실한 게으른 무자비한 소심한 우아한 화난 배고픈
        수줍은 냉정한 뜨거운 은밀한 눈치없는 지각하는 도망친 부지런한
        산만한 진지한 낙천적인 예민한 폭주하는 방황하는 침착한 엄격한
        능청스런 태연한 굶주린 겸손한 까칠한 유쾌한 근면한 초조한
        """.trimIndent().split(" ", "\n")

    internal val techTerms =
        """
        SegFault Regex Nullable Kubernetes Deadlock Merge Rebase Hotfix
        Docker Redis Webpack Compiler Linter Daemon Pointer Cache
        Mutex Semaphore Callback Closure Promise Thread Kernel Cronjob
        Sandbox Firewall Payload Endpoint Migration Rollback Backlog Bytecode
        Heap Stack Monolith Refactor Timeout Latency Pipeline Snapshot
        Debugger Recursion NPE Legacy Nginx Lambda
        """.trimIndent().split(" ", "\n")

    /** [random]은 테스트에서 시드를 고정하려고 열어 둔 자리입니다. 호출부는 인자 없이 씁니다. */
    fun generate(random: Random = Random): String = "${adjectives.random(random)} ${techTerms.random(random)}"
}
