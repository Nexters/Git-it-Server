package com.nexters.gitit.application.member

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.member.MemberWithdrawn
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class WithdrawMember(
    private val memberRepository: MemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * 소프트 삭제가 아니라 하드 삭제입니다. 회원 문서만 지우고, 그 회원에 매달린 데이터는
     * [MemberWithdrawn]를 받는 쪽이 비동기로 지웁니다 — 탈퇴 응답이 정리 시간만큼 늦어지지 않습니다.
     */
    operator fun invoke(command: Command) {
        memberRepository.findById(command.memberId) ?: throw BaseException(ErrorCode.MEMBER_NOT_FOUND)

        memberRepository.deleteById(command.memberId)
        eventPublisher.publishEvent(MemberWithdrawn(command.memberId))
    }

    data class Command(
        val memberId: String,
    )
}
