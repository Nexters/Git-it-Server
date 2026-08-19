package com.nexters.gitit.application.member

import com.nexters.gitit.domain.member.Member
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.member.MemberWithdrawn
import com.nexters.gitit.domain.member.SocialIdentity
import com.nexters.gitit.domain.member.SocialType
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher

class WithdrawMemberTest {
    private val memberRepository: MemberRepository = mock()
    private val eventPublisher: ApplicationEventPublisher = mock()

    private val withdrawMember = WithdrawMember(memberRepository, eventPublisher)

    @Test
    fun `회원을 지우고 딸린 데이터를 정리하라고 알린다`() {
        val member =
            Member(
                socialIdentity = SocialIdentity("withdrawing-member", SocialType.GOOGLE),
                email = "gitit@nexters.com",
                name = "겁없는 SegFault",
            )
        whenever(memberRepository.findById(member.id)).thenReturn(member)

        withdrawMember(WithdrawMember.Command(member.id))

        verify(memberRepository).deleteById(member.id)
        verify(eventPublisher).publishEvent(MemberWithdrawn(member.id))
    }
}
