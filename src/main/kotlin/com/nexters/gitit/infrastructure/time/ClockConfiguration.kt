package com.nexters.gitit.infrastructure.time

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class ClockConfiguration {
    /**
     * 서버 기본 타임존에 의존하면 로컬과 배포 환경의 날짜 경계가 어긋나므로 KST를 명시합니다.
     */
    @Bean
    fun clock(): Clock = Clock.system(KST)

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
