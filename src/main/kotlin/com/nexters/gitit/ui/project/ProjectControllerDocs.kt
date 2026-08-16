package com.nexters.gitit.ui.project

import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.project.dto.ProjectDetailResponse
import com.nexters.gitit.ui.project.dto.ProjectListResponse
import com.nexters.gitit.ui.project.dto.RegisterProjectRequest
import com.nexters.gitit.ui.project.dto.RegisterProjectResponse
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
        summary = "프로젝트 등록",
        description =
            "GitHub 저장소를 학습할 프로젝트로 등록합니다. 문제는 등록 직후가 아니라 몇 분 뒤에 채워지므로, 응답의 status가 " +
                "완료 상태가 될 때까지는 아직 풀 문제가 없습니다. 다른 회원이 이미 등록한 저장소라면 그 문제 세트를 함께 씁니다. " +
                "같은 저장소를 다시 등록해도 프로젝트가 새로 생기지 않고 기존 프로젝트가 그대로 돌아오며, 난이도도 바뀌지 않습니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "등록 성공",
        ),
        SwaggerApiResponse(
            responseCode = "400",
            description = "githubRepoUrl이 비어 있거나, GitHub에 없는 저장소이거나, 문제를 낼 수 없다고 판정된 저장소",
            content = [
                Content(
                    mediaType = APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(name = "필수 값 누락", value = INVALID_INPUT_EXAMPLE),
                        ExampleObject(name = "등록할 수 없는 저장소", value = INVALID_REPOSITORY_EXAMPLE),
                    ],
                ),
            ],
        ),
    )
    fun registerProject(
        memberId: String,
        request: RegisterProjectRequest,
    ): ApiResponse<RegisterProjectResponse>

    @Operation(
        summary = "프로젝트 목록 조회",
        description = "내가 학습 중인 프로젝트 목록을 생성 순서(오래된 순)로 조회합니다.",
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
        SwaggerApiResponse(
            responseCode = "404",
            description = "존재하지 않거나 본인 소유가 아니거나 삭제된 프로젝트",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = NOT_FOUND_EXAMPLE)])],
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
        SwaggerApiResponse(
            responseCode = "404",
            description = "존재하지 않거나 본인 소유가 아니거나 이미 삭제된 프로젝트",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = NOT_FOUND_EXAMPLE)])],
        ),
    )
    fun deleteProject(
        memberId: String,
        @Parameter(description = "삭제할 프로젝트 ID") projectId: String,
    ): ApiResponse<Unit>

    companion object {
        // 401은 OpenApiConfig의 loginMemberSecurityCustomizer가 @LoginMember 파라미터를 보고 자동으로 붙이므로 여기 적지 않습니다.
        private const val INVALID_INPUT_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"잘못된 요청입니다","errors":[{"field":"githubRepoUrl","message":"githubRepoUrl은 필수입니다"}]}"""

        // URL 형식이 틀린 것과 GitHub에 없는 것을 구분하지 않습니다. 사용자가 할 일은 어느 쪽이든 URL을 다시 확인하는 것이라 같습니다.
        private const val INVALID_REPOSITORY_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"유효하지 않은 GitHub 저장소입니다","errors":null}"""

        private const val NOT_FOUND_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-004","message":"프로젝트를 찾을 수 없습니다","errors":null}"""
    }
}
