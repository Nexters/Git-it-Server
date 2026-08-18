package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.GetQuizGenerationStatus
import com.nexters.gitit.domain.quizrepo.QuizRepoStatus
import io.swagger.v3.oas.annotations.media.Schema

data class QuizGenerationStatusResponse(
    @field:Schema(description = "문제 생성 진행 상태. COMPLETED가 아니면 아직 풀 수 있는 문제가 없습니다")
    val status: QuizRepoStatus,
) {
    companion object {
        fun from(result: GetQuizGenerationStatus.Result) = QuizGenerationStatusResponse(forClient(result.status))

        // STARTED는 서버가 그 저장소를 쥐고 있다는 표식일 뿐이라, 기다리는 쪽에서는 READY와 다를 것이 없다.
        fun forClient(status: QuizRepoStatus) = if (status == QuizRepoStatus.STARTED) QuizRepoStatus.READY else status
    }
}
