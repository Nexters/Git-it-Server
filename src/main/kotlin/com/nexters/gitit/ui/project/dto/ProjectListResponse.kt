package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.GetProjects
import io.swagger.v3.oas.annotations.media.Schema

data class ProjectListResponse(
    val items: List<ProjectItemResponse>,
    @field:Schema(description = "다음 페이지 존재 여부")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: GetProjects.Result) =
            ProjectListResponse(
                items = result.items.map { ProjectItemResponse.from(it) },
                hasNext = result.hasNext,
            )
    }
}

data class ProjectItemResponse(
    val projectId: String,
    @field:Schema(description = "레포지토리 프로필 이미지")
    val repositoryImageUrl: String?,
    @field:Schema(description = "레포지토리 이름")
    val repositoryName: String,
    @field:Schema(description = "기술 스택")
    val techStack: List<String>,
    @field:Schema(description = "다음에 풀 문제가 속한 세트 라벨 (예: Set 1)")
    val currentSetLabel: String,
    @field:Schema(description = "다음에 풀 문제가 속한 세트 제목")
    val currentSetTitle: String,
    @field:Schema(description = "다음에 풀어야 할 문제 ID. 재생 버튼 클릭 시 이 문제로 이동")
    val nextProblemId: String?,
    @field:Schema(description = "프로젝트 전체 진행률(%)")
    val overallProgressPercent: Int,
) {
    companion object {
        fun from(item: GetProjects.ProjectItem) =
            ProjectItemResponse(
                projectId = item.projectId,
                repositoryImageUrl = item.repositoryImageUrl,
                repositoryName = item.repositoryName,
                techStack = item.techStack,
                currentSetLabel = item.currentSetLabel,
                currentSetTitle = item.currentSetTitle,
                nextProblemId = item.nextProblemId,
                overallProgressPercent = item.overallProgressPercent,
            )
    }
}
