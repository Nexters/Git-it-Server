package com.nexters.gitit.infrastructure.async

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/**
 * 이게 없으면 `@Async`가 조용히 무시됩니다 — 애너테이션만 남고 프록시가 안 붙어
 * 문제 생성이 이벤트 발행 스레드에서 그대로 돌고, 응답이 2~5분 뒤에 나갑니다.
 *
 * 실행기는 부트 기본값을 씁니다. 스레드 풀을 손봐야 할 근거(동시 생성 요청 실측)가 아직 없습니다.
 */
@Configuration
@EnableAsync
class AsyncConfiguration
