package com.nexters.gitit.scheduler

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * 이게 없으면 `@Scheduled`가 조용히 무시됩니다 — 애너테이션만 남고 아무것도 돌지 않습니다.
 *
 * 잡 클래스에 얹지 않고 따로 두는 것은, 잡은 프로필이나 설정으로 꺼질 수 있는데 그때 스케줄링 자체가
 * 같이 꺼지면 나머지 잡까지 말없이 멈추기 때문입니다.
 */
@Configuration
@EnableScheduling
class SchedulingConfiguration
