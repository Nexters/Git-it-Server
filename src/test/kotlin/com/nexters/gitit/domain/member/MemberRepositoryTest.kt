package com.nexters.gitit.domain.member

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.infrastructure.mongo.MongoAuditingConfiguration
import com.nexters.gitit.infrastructure.mongo.SpringDataMemberRepository
import com.nexters.gitit.infrastructure.time.ClockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
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
import org.springframework.data.mongodb.core.indexOps
import java.time.Clock
import java.time.temporal.ChronoUnit

@DataMongoTest
// @DataMongoTest 슬라이스는 일반 @Configuration을 스캔하지 않으므로 감사 설정을 직접 넣는다.
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
    fun `socialIdentity unique 인덱스가 실제로 생성된다`() {
        // 위 unique 테스트가 인덱스 부재가 아닌 다른 이유로 통과/실패하는 상황을 배제한다.
        val index =
            mongoTemplate
                .indexOps<Member>()
                .indexInfo
                .find { it.name == "uk_social_identity" }
                .shouldNotBeNull()

        index.isUnique shouldBe true
        index.indexFields.map { it.key } shouldContainExactly listOf("socialIdentity.socialId", "socialIdentity.socialType")
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
        memberRepository.findById(saved.id).orElse(null).shouldNotBeNull().deletedAt.shouldNotBeNull()
    }

    private fun memberOf(
        socialIdentity: SocialIdentity,
        email: String? = null,
    ) = Member(socialIdentity = socialIdentity, email = email)
}
