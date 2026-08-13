package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.GetProjectRepositoryUrl
import io.swagger.v3.oas.annotations.media.Schema

data class ProjectRepositoryUrlResponse(
    @field:Schema(description = "GitHub 레포지토리 링크. 아직 프로젝트 생성 기능이 이 값을 채우지 않아 null일 수 있음")
    val repositoryUrl: String?,
) {
    companion object {
        fun from(result: GetProjectRepositoryUrl.Result) = ProjectRepositoryUrlResponse(repositoryUrl = result.repositoryUrl)
    }
}
