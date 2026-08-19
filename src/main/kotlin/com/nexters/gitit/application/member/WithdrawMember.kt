package com.nexters.gitit.application.member

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.project.ProjectRepository
import org.springframework.stereotype.Service

@Service
class WithdrawMember(
    private val memberRepository: MemberRepository,
    private val projectRepository: ProjectRepository,
) {
    /**
     * 소프트 삭제가 아니라 하드 삭제입니다. QuizRepo는 여러 회원이 함께 쓰는 공용 자원이라 건드리지 않고,
     * 그 회원의 Project(학습 진도·답변·북마크)까지만 함께 지웁니다.
     *
     * Project를 먼저 지우는 이유는, 중간에 실패해도 회원 문서가 남아 있어야 "탈퇴가 덜 끝났다"는 걸
     * 알 수 있기 때문입니다. 회원을 먼저 지우면 실패 시 주인 없는 Project만 남고 아무도 그걸 모릅니다.
     */
    operator fun invoke(command: Command) {
        memberRepository.findById(command.memberId) ?: throw BaseException(ErrorCode.MEMBER_NOT_FOUND)

        projectRepository.deleteAllByMemberId(command.memberId)
        memberRepository.deleteById(command.memberId)
    }

    data class Command(
        val memberId: String,
    )
}
