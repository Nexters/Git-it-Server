package com.nexters.gitit.infrastructure.mongo

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.mongodb.config.EnableMongoAuditing
import java.time.Clock
import java.time.LocalDateTime
import java.util.Optional

@Configuration
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class MongoAuditingConfiguration {
    /**
     * 기본 제공자는 시스템 시계를 직접 읽어 ClockConfiguration이 고정한 KST와 어긋납니다.
     */
    @Bean
    fun auditingDateTimeProvider(clock: Clock) = DateTimeProvider { Optional.of(LocalDateTime.now(clock)) }
}
