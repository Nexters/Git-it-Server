package com.nexters.gitit.ui.project

import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.project.dto.ProjectDetailResponse
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

    @Operation(
        summary = "프로젝트 상세 조회",
        description = "프로젝트 상세 정보를 조회합니다. 레포 정보(GitHub 링크 포함), 전체 진행률, 다음 문제 ID, 세트별 진행 현황을 반환합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "404",
            description = "존재하지 않거나 본인 소유가 아니거나 삭제된 프로젝트",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = NOT_FOUND_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = UNAUTHORIZED_EXAMPLE)])],
        ),
    )
    fun getProjectDetail(
        memberId: String,
        @Parameter(description = "조회할 프로젝트 ID") projectId: String,
    ): ApiResponse<ProjectDetailResponse>

    @Operation(
        summary = "프로젝트 삭제",
        description = "프로젝트를 소프트 삭제합니다. 본인 소유가 아니거나 이미 삭제된 경우 존재 여부를 노출하지 않기 위해 404로 응답합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "삭제 성공"),
        SwaggerApiResponse(
            responseCode = "404",
            description = "존재하지 않거나 본인 소유가 아니거나 이미 삭제된 프로젝트",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = NOT_FOUND_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = UNAUTHORIZED_EXAMPLE)])],
        ),
    )
    fun deleteProject(
        memberId: String,
        @Parameter(description = "삭제할 프로젝트 ID") projectId: String,
    ): ApiResponse<Unit>

    companion object {
        private const val UNAUTHORIZED_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-002","message":"인증이 필요합니다","errors":null}"""
        private const val NOT_FOUND_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-004","message":"프로젝트를 찾을 수 없습니다","errors":null}"""
    }
}
