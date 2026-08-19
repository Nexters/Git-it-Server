package com.nexters.gitit.application.member

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.member.Position
import org.springframework.stereotype.Service

@Service
class UpdateMemberPosition(
    private val memberRepository: MemberRepository,
) {
    /**
     * 토큰은 유효한데 회원이 없는 경우는 탈퇴 후 만료 전 토큰으로 호출한 경우뿐이라 401이 아닌 404로 봅니다.
     */
    operator fun invoke(command: Command) {
        val member = memberRepository.findById(command.memberId) ?: throw BaseException(ErrorCode.MEMBER_NOT_FOUND)

        member.updatePosition(command.position)
        memberRepository.save(member)
    }

    data class Command(
        val memberId: String,
        val position: Position,
    )
}
