package com.nexters.gitit.domain.common

import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.Persistable
import java.time.Clock
import java.time.Instant

abstract class BaseEntity : Persistable<String> {
    // private은 getId()와의 JVM 시그니처 충돌 회피용, var는 조회 시 Spring Data가 _id를 되써야 하기 때문.
    @Id
    private var id: String = ObjectId().toString()

    @CreatedDate
    var createdAt: Instant? = null
        private set

    @LastModifiedDate
    var updatedAt: Instant? = null
        private set

    var deletedAt: Instant? = null

    fun delete(clock: Clock) {
        deletedAt = Instant.now(clock)
    }

    /**
     * id를 앱에서 미리 만들어 넣으므로 Spring Data 기본 판정(id == null)으로는 항상 기존 도큐먼트가 되어
     * `@CreatedDate`가 조용히 건너뛰어집니다. [createdAt]은 첫 저장 때만 채워지므로 이걸로 대신 판정합니다.
     */
    override fun isNew(): Boolean = createdAt == null

    // Kotlin은 Java 인터페이스의 getId()를 프로퍼티로 오버라이드할 수 없어 함수로 구현한다.
    override fun getId(): String = id
}
