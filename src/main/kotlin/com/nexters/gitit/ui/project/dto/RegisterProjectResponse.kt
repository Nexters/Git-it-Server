package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.RegisterProject
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import io.swagger.v3.oas.annotations.media.Schema

data class RegisterProjectResponse(
    @field:Schema(description = "등록된 프로젝트 id. 이 값으로 프로젝트 상세를 조회합니다")
    val projectId: String,
    @field:Schema(description = "문제 생성 진행 상태. 갓 등록했다면 READY이고 문제는 몇 분 뒤에 채워집니다. COMPLETED가 아니면 아직 풀 수 있는 문제가 없습니다")
    val status: QuizRepoStatus,
) {
    companion object {
        fun from(result: RegisterProject.Result) =
            RegisterProjectResponse(
                projectId = result.projectId,
                // STARTED는 서버가 그 저장소를 쥐고 있다는 표식일 뿐이라, 기다리는 쪽에서는 READY와 다를 것이 없다.
                status = if (result.status == QuizRepoStatus.STARTED) QuizRepoStatus.READY else result.status,
            )
    }
}
