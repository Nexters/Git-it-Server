package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.project.RegisterProject
import com.nexters.gitit.domain.quizrepo.Depth
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * quizLevel에는 검증 애너테이션을 붙이지 않습니다. 실제 enum이라 정의에 없는 값이 오면 본문을 읽는 단계에서
 * 이미 걸리고, 그 결과도 똑같이 400이라 검증을 한 겹 더 두면 같은 판정을 두 곳에서 관리하게 됩니다.
 */
data class RegisterProjectRequest(
    @field:Schema(description = "등록할 GitHub 저장소 URL", example = "https://github.com/Nexters/Git-it-Server")
    @field:NotBlank(message = "githubRepoUrl은 필수입니다")
    val githubRepoUrl: String,
    @field:Schema(description = "풀고 싶은 문제 난이도 - 깊이만 나누고 직급과는 무관합니다", example = "L2")
    val quizLevel: Depth,
) {
    fun toCommand(memberId: String) =
        RegisterProject.Command(
            memberId = memberId,
            githubRepoUrl = githubRepoUrl,
            quizLevel = quizLevel,
        )
}
