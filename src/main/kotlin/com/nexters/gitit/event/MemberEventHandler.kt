package com.nexters.gitit.event

import com.nexters.gitit.application.project.DeleteMemberProjects
import com.nexters.gitit.domain.member.MemberWithdrawn
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * 회원 도메인의 이벤트 수신 지점. 탈퇴에 딸린 정리 작업은 전부 여기에 줄지어 둡니다 —
 * 회원에 매달리는 도메인이 늘어도 [com.nexters.gitit.application.member.WithdrawMember]는 그대로입니다.
 */
@Component
class MemberEventHandler(
    private val deleteMemberProjects: DeleteMemberProjects,
) {
    @Async
    @EventListener
    fun handle(event: MemberWithdrawn) {
        deleteMemberProjects(DeleteMemberProjects.Command(event.memberId))
    }
}
