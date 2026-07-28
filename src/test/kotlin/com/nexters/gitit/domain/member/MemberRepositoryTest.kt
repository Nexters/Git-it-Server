package com.nexters.gitit.domain.member

import com.nexters.gitit.TestcontainersConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
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

@DataMongoTest
@Import(TestcontainersConfiguration::class)
class MemberRepositoryTest(
    @Autowired private val memberRepository: MemberRepository,
    @Autowired private val mongoTemplate: MongoTemplate,
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

        val found = memberRepository.findBySocialIdentity(socialIdentity).shouldNotBeNull()

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

        memberRepository.findBySocialIdentity(socialIdentity).shouldNotBeNull().email shouldBe "first@nexters.com"
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
        index.indexFields.map { it.key } shouldContainExactly
            listOf("socialIdentity.socialId", "socialIdentity.socialType")
    }

    private fun memberOf(
        socialIdentity: SocialIdentity,
        email: String? = null,
    ) = Member(socialIdentity = socialIdentity, email = email)
}
