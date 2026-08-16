package com.nexters.gitit.ui.project

import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.project.dto.RegisterProjectRequest
import com.nexters.gitit.ui.project.dto.RegisterProjectResponse
import io.swagger.v3.oas.annotations.Operation
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

    companion object {
        // 401은 OpenApiConfig의 loginMemberSecurityCustomizer가 @LoginMember 파라미터를 보고 자동으로 붙이므로 여기 적지 않습니다.
        private const val INVALID_INPUT_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"잘못된 요청입니다","errors":[{"field":"githubRepoUrl","message":"githubRepoUrl은 필수입니다"}]}"""

        // URL 형식이 틀린 것과 GitHub에 없는 것을 구분하지 않습니다. 사용자가 할 일은 어느 쪽이든 URL을 다시 확인하는 것이라 같습니다.
        private const val INVALID_REPOSITORY_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"유효하지 않은 GitHub 저장소입니다","errors":null}"""
    }
}
