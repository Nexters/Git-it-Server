package com.nexters.gitit.ui.project

import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.project.dto.ProjectListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Project", description = "프로젝트 API")
interface ProjectControllerDocs {
    @Operation(
        summary = "프로젝트 목록 조회",
        description = "내가 학습 중인 프로젝트 목록을 생성 순서(오래된 순)로 조회합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = UNAUTHORIZED_EXAMPLE)])],
        ),
    )
    fun getProjects(
        memberId: String,
        @Parameter(description = "페이지 번호 (0부터 시작)") page: Int,
        @Parameter(description = "페이지 크기") size: Int,
    ): ApiResponse<ProjectListResponse>

    companion object {
        private const val UNAUTHORIZED_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-002","message":"인증이 필요합니다","errors":null}"""
    }
}
