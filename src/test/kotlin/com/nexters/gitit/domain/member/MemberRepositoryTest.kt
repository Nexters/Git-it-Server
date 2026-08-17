package com.nexters.gitit.domain.member

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.infrastructure.mongo.MongoAuditingConfiguration
import com.nexters.gitit.infrastructure.mongo.SpringDataMemberRepository
import com.nexters.gitit.infrastructure.time.ClockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.Clock
import java.time.temporal.ChronoUnit

@DataMongoTest
@Import(TestcontainersConfiguration::class, MongoAuditingConfiguration::class, ClockConfiguration::class)
class MemberRepositoryTest(
    @Autowired private val memberRepository: SpringDataMemberRepository,
    @Autowired private val mongoTemplate: MongoTemplate,
    @Autowired private val clock: Clock,
) {
    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        memberRepository.deleteAll()
    }

    @Test
    fun `저장한 회원을 socialIdentity로 조회할 수 있다`() {
        val socialIdentity = SocialIdentity("google-social-id", SocialType.GOOGLE)
        val saved = memberRepository.save(memberOf(socialIdentity, email = "gitit@nexters.com"))

        val found = memberRepository.findBySocialIdentityAndDeletedAtIsNull(socialIdentity).shouldNotBeNull()

        found.id shouldBe saved.id
        found.socialIdentity shouldBe socialIdentity
        found.email shouldBe "gitit@nexters.com"
    }

    @Test
    fun `같은 socialIdentity를 가진 회원을 두 번 저장하면 두 번째가 실패한다`() {
        val socialIdentity = SocialIdentity("duplicated-social-id", SocialType.GOOGLE)
        memberRepository.save(memberOf(socialIdentity, email = "first@nexters.com"))

        // id를 재사용하면 같은 도큐먼트의 갱신이 되어버리므로, 별개의 Member 인스턴스로 저장한다.
        shouldThrow<DuplicateKeyException> {
            memberRepository.save(memberOf(socialIdentity, email = "second@nexters.com"))
        }

        memberRepository.findBySocialIdentityAndDeletedAtIsNull(socialIdentity).shouldNotBeNull().email shouldBe "first@nexters.com"
    }

    @Test
    fun `탈퇴한 회원과 같은 socialIdentity로 다시 가입할 수 있다`() {
        val socialIdentity = SocialIdentity("rejoin-social-id", SocialType.GOOGLE)
        val first = memberRepository.save(memberOf(socialIdentity, email = "first@nexters.com"))
        memberRepository.save(first.apply { delete(clock) })

        val rejoined = memberRepository.save(memberOf(socialIdentity, email = "second@nexters.com"))

        memberRepository.findBySocialIdentityAndDeletedAtIsNull(socialIdentity).shouldNotBeNull().id shouldBe rejoined.id
        // 탈퇴 이력은 지워지지 않아야 한다.
        memberRepository
            .findById(first.id)
            .orElse(null)
            .shouldNotBeNull()
            .deletedAt
            .shouldNotBeNull()
    }

    @Test
    fun `같은 socialIdentity로 여러 번 탈퇴한 이력이 남아도 저장할 수 있다`() {
        // partial 인덱스가 아니라 deletedAt을 키에 넣는 방식이면, 같은 밀리초에 삭제된 도큐먼트끼리 충돌한다.
        val socialIdentity = SocialIdentity("multi-withdrawal-social-id", SocialType.GOOGLE)
        repeat(3) {
            memberRepository.save(memberOf(socialIdentity).apply { delete(clock) })
        }

        memberRepository.findBySocialIdentityAndDeletedAtIsNull(socialIdentity).shouldBeNull()
        mongoTemplate.count(Query(Criteria.where("socialIdentity.socialId").`is`(socialIdentity.socialId)), Member::class.java) shouldBe 3
    }

    @Test
    fun `재가입 이후에도 활성 회원은 여전히 한 명만 존재할 수 있다`() {
        val socialIdentity = SocialIdentity("rejoin-then-duplicate", SocialType.GOOGLE)
        memberRepository.save(memberOf(socialIdentity).apply { delete(clock) })
        memberRepository.save(memberOf(socialIdentity))

        shouldThrow<DuplicateKeyException> {
            memberRepository.save(memberOf(socialIdentity))
        }
    }

    @Test
    fun `회원을 저장하면 createdAt과 updatedAt이 채워진다`() {
        val member = memberOf(SocialIdentity("audit-insert", SocialType.GOOGLE))
        // 저장 전에는 감사 값이 비어 있어야 isNew 판정이 신규로 잡힌다.
        member.createdAt.shouldBeNull()

        val saved = memberRepository.save(member)

        val found = memberRepository.findById(saved.id).orElse(null).shouldNotBeNull()
        found.createdAt.shouldNotBeNull()
        found.updatedAt.shouldNotBeNull()
        found.deletedAt.shouldBeNull()
    }

    @Test
    fun `회원을 다시 저장하면 createdAt은 유지되고 updatedAt만 갱신된다`() {
        val saved = memberRepository.save(memberOf(SocialIdentity("audit-update", SocialType.GOOGLE)))
        val createdAt = saved.createdAt.shouldNotBeNull().truncatedTo(ChronoUnit.MILLIS)
        val updatedAt = saved.updatedAt.shouldNotBeNull()

        // LastModifiedDate는 밀리초 단위로 저장되므로, 같은 밀리초에 두 번 저장되는 상황을 배제한다.
        Thread.sleep(10)
        // DB에서 다시 읽은 인스턴스를 저장해야 createdAt이 메모리 값이 아닌 저장된 값에서 온 것임을 확인할 수 있다.
        memberRepository.save(memberRepository.findById(saved.id).orElse(null).shouldNotBeNull())

        val found = memberRepository.findById(saved.id).orElse(null).shouldNotBeNull()
        found.createdAt shouldBe createdAt
        found.updatedAt.shouldNotBeNull() shouldBeAfter updatedAt
    }

    @Test
    fun `탈퇴한 회원은 socialIdentity로 조회되지 않는다`() {
        val socialIdentity = SocialIdentity("deleted-member", SocialType.GOOGLE)
        val member = memberOf(socialIdentity).apply { delete(clock) }
        val saved = memberRepository.save(member)

        memberRepository.findBySocialIdentityAndDeletedAtIsNull(socialIdentity).shouldBeNull()

        // soft delete이므로 도큐먼트 자체는 남아 있어야 한다.
        memberRepository
            .findById(saved.id)
            .orElse(null)
            .shouldNotBeNull()
            .deletedAt
            .shouldNotBeNull()
    }

    @Test
    fun `기기 정보는 중첩 도큐먼트로 저장돼 하위 필드로 조회할 수 있다`() {
        // 읽기만 확인하면 평탄화돼 저장돼도 통과한다. 푸시 대상을 deviceInfo.deviceToken으로 거를 때 드러날 차이라
        // 값 객체가 중첩 도큐먼트로 들어갔는지를 여기서 고정한다.
        val member = memberOf(SocialIdentity("device-owner", SocialType.GOOGLE))
        member.updateDeviceInfo(
            DeviceInfo(
                deviceId = "device-1",
                deviceType = "ios",
                appVersion = "1.0.0",
                osVersion = "18.2",
                deviceToken = "device-token",
            ),
        )
        memberRepository.save(member)

        val query = Query(Criteria.where("deviceInfo.deviceToken").`is`("device-token"))

        mongoTemplate.count(query, Member::class.java) shouldBe 1
    }

    private fun memberOf(
        socialIdentity: SocialIdentity,
        email: String? = null,
    ) = Member(socialIdentity = socialIdentity, email = email, name = "겁없는 SegFault")
}
