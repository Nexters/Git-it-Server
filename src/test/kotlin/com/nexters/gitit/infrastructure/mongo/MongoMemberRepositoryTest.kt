package com.nexters.gitit.infrastructure.mongo

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.member.Member
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.member.SocialIdentity
import com.nexters.gitit.domain.member.SocialType
import com.nexters.gitit.infrastructure.time.ClockConfiguration
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.context.annotation.Import
import java.time.Clock

/**
 * 포트가 소프트 삭제를 감춘다는 규약을 지키는지 확인합니다.
 * 조건이 SpringDataMemberRepository의 메서드 이름에만 있어서, 어댑터가 다른 메서드를 부르면
 * 컴파일은 통과하고 탈퇴 회원이 조용히 조회됩니다.
 */
@DataMongoTest
@Import(
    TestcontainersConfiguration::class,
    MongoAuditingConfiguration::class,
    ClockConfiguration::class,
    MongoMemberRepository::class,
)
class MongoMemberRepositoryTest(
    @Autowired private val memberRepository: MemberRepository,
    @Autowired private val springDataMemberRepository: SpringDataMemberRepository,
    @Autowired private val clock: Clock,
) {
    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        springDataMemberRepository.deleteAll()
    }

    @Test
    fun `저장한 회원을 id로 조회할 수 있다`() {
        val saved = memberRepository.save(memberOf("find-by-id"))

        val found = memberRepository.findById(saved.id).shouldNotBeNull()

        found.id shouldBe saved.id
        found.socialIdentity shouldBe saved.socialIdentity
    }

    @Test
    fun `탈퇴한 회원은 id로 조회되지 않는다`() {
        val saved = memberRepository.save(memberOf("deleted-find-by-id"))
        memberRepository.save(saved.apply { delete(clock) })

        memberRepository.findById(saved.id).shouldBeNull()

        // soft delete이므로 도큐먼트 자체는 남아 있어야 한다.
        springDataMemberRepository
            .findById(saved.id)
            .orElse(null)
            .shouldNotBeNull()
            .deletedAt
            .shouldNotBeNull()
    }

    private fun memberOf(socialId: String) =
        Member(
            socialIdentity = SocialIdentity(socialId, SocialType.GOOGLE),
            email = "gitit@nexters.com",
            name = "겁없는 SegFault",
        )
}
