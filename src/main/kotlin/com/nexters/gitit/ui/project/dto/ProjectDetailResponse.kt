package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.GetProjectDetail
import io.swagger.v3.oas.annotations.media.Schema

data class ProjectDetailResponse(
    val projectId: String,
    @field:Schema(description = "GitHub 레포지토리 링크")
    val repositoryUrl: String,
    @field:Schema(description = "레포지토리 이름")
    val repositoryName: String,
    @field:Schema(description = "레포지토리 소유자 프로필 이미지")
    val repositoryImageUrl: String,
    @field:Schema(description = "GitHub 스타 수")
    val starCount: Int,
    @field:Schema(description = "기술 스택")
    val techStack: List<String>,
    @field:Schema(description = "프로젝트 전체 진행률(%)")
    val overallProgressPercent: Int,
    @field:Schema(description = "다음에 풀어야 할 문제 ID. 재생 버튼 클릭 시 이 문제로 이동")
    val nextQuestionId: String?,
    val sets: List<SetResponse>,
) {
    companion object {
        fun from(result: GetProjectDetail.Result) =
            ProjectDetailResponse(
                projectId = result.projectId,
                repositoryUrl = result.repositoryUrl,
                repositoryName = result.repositoryName,
                repositoryImageUrl = result.repositoryImageUrl,
                starCount = result.starCount,
                techStack = result.techStack,
                overallProgressPercent = result.overallProgressPercent,
                nextQuestionId = result.nextQuestionId,
                sets = result.sets.map { SetResponse.from(it) },
            )
    }
}

data class SetResponse(
    val setId: String,
    @field:Schema(description = "세트 라벨 (예: \"Set 1\")")
    val label: String,
    @field:Schema(description = "세트 제목")
    val title: String,
    @field:Schema(description = "세트 내 문제 개수")
    val problemCount: Int,
    @field:Schema(description = "세트 내 정답 제출한 문제 개수")
    val completedCount: Int,
) {
    companion object {
        fun from(item: GetProjectDetail.SetItem) =
            SetResponse(
                setId = item.setId,
                label = item.label,
                title = item.title,
                problemCount = item.problemCount,
                completedCount = item.completedCount,
            )
    }
}
