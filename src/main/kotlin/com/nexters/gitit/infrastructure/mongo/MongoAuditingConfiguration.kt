package com.nexters.gitit.infrastructure.mongo

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.mongodb.config.EnableMongoAuditing
import java.time.Clock
import java.time.Instant
import java.util.Optional

@Configuration
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class MongoAuditingConfiguration {
    /**
     * 기본 제공자는 시스템 시계를 직접 읽어 주입된 Clock(테스트에서 고정하는)과 어긋납니다.
     */
    @Bean
    fun auditingDateTimeProvider(clock: Clock) = DateTimeProvider { Optional.of(Instant.now(clock)) }
}
