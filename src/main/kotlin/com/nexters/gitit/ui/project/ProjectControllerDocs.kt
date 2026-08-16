package com.nexters.gitit.ui.project

import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.project.dto.BookmarkQuestionRequest
import com.nexters.gitit.ui.project.dto.BookmarkQuestionResponse
import com.nexters.gitit.ui.project.dto.BookmarkedQuestionListResponse
import com.nexters.gitit.ui.project.dto.LearningSetResponse
import com.nexters.gitit.ui.project.dto.ProjectDetailResponse
import com.nexters.gitit.ui.project.dto.ProjectListResponse
import com.nexters.gitit.ui.project.dto.RegisterProjectRequest
import com.nexters.gitit.ui.project.dto.RegisterProjectResponse
import com.nexters.gitit.ui.project.dto.SubmitChoiceAnswerRequest
import com.nexters.gitit.ui.project.dto.SubmitChoiceAnswerResponse
import com.nexters.gitit.ui.project.dto.SubmitEssayAnswerRequest
import com.nexters.gitit.ui.project.dto.SubmitEssayAnswerResponse
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
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = PROJECT_LIST_EXAMPLE)])],
        ),
    )
    fun getProjects(
        memberId: String,
        @Parameter(description = "페이지 번호 (0부터 시작)") page: Int,
        @Parameter(description = "페이지 크기") size: Int,
    ): ApiResponse<ProjectListResponse>

    @Operation(
        summary = "북마크한 문제 목록 조회",
        description =
            "내가 북마크한 문제를 프로젝트별로 필터링해 조회합니다. projectId를 안 주면 전체입니다. " +
                "availableProjects는 필터와 무관하게 북마크가 있는 프로젝트 전부라, 필터 칩 목록을 그릴 때 씁니다.",
    )
    fun getBookmarkedQuestions(
        memberId: String,
        @Parameter(description = "특정 프로젝트로 필터링. 생략하면 전체 프로젝트") projectId: String?,
    ): ApiResponse<BookmarkedQuestionListResponse>

    @Operation(
        summary = "프로젝트 상세 조회",
        description = "프로젝트 상세 정보를 조회합니다. 레포 정보(GitHub 링크 포함), 전체 진행률, 다음 문제 ID, 세트별 진행 현황을 반환합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = PROJECT_DETAIL_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "존재하지 않거나 본인 소유가 아니거나 삭제된 프로젝트",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = PROJECT_NOT_FOUND_EXAMPLE)])],
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
            responseCode = "200",
            description = "삭제 성공",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = SUCCESS_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "존재하지 않거나 본인 소유가 아니거나 이미 삭제된 프로젝트",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = PROJECT_NOT_FOUND_EXAMPLE)])],
        ),
    )
    fun deleteProject(
        memberId: String,
        @Parameter(description = "삭제할 프로젝트 ID") projectId: String,
    ): ApiResponse<Unit>

    @Operation(
        summary = "학습 세트 조회",
        description =
            "세트에 걸린 문제를 프로젝트 난이도에 맞춰 돌려줍니다. 이미 푼 문제도 걸러내지 않고 만들어진 순서 그대로 나가며, " +
                "푼 문제에는 그때 낸 답(myAnswer)이 붙습니다 — 이어 풀 지점은 myAnswer가 없는 첫 문제입니다. " +
                "정답·해설·채점 기준은 답변 제출 응답으로만 나갑니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
        SwaggerApiResponse(
            responseCode = "404",
            description = "내 프로젝트가 아니거나, 그 프로젝트의 저장소에 없는 학습 세트. 문제 생성이 아직 안 끝난 저장소도 여기에 해당합니다",
            content = [
                Content(
                    mediaType = APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(name = "없는 프로젝트", value = PROJECT_NOT_FOUND_EXAMPLE),
                        ExampleObject(name = "없는 학습 세트", value = LEARNING_SET_NOT_FOUND_EXAMPLE),
                    ],
                ),
            ],
        ),
    )
    fun getLearningSet(
        memberId: String,
        projectId: String,
        setId: String,
    ): ApiResponse<LearningSetResponse>

    @Operation(
        summary = "4지선다 답변 제출",
        description =
            "고른 선택지를 제출하고 채점 결과를 받습니다. 같은 문제를 다시 풀면 답이 쌓이지 않고 마지막 것으로 덮어써집니다. " +
                "서술형 문제 id로 이 경로를 부르면 400입니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "제출 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "선택지 범위를 벗어난 번호이거나, 서술형 문제에 4지선다로 답한 경우",
            content = [
                Content(
                    mediaType = APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(name = "선택지 범위 밖", value = OUT_OF_CHOICE_RANGE_EXAMPLE),
                        ExampleObject(name = "형식이 다른 문제", value = FORMAT_MISMATCH_EXAMPLE),
                    ],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "내 프로젝트가 아니거나, 그 프로젝트의 저장소에 없는 문제",
            content = [
                Content(
                    mediaType = APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(name = "없는 프로젝트", value = PROJECT_NOT_FOUND_EXAMPLE),
                        ExampleObject(name = "없는 문제", value = QUESTION_NOT_FOUND_EXAMPLE),
                    ],
                ),
            ],
        ),
    )
    fun submitChoiceAnswer(
        memberId: String,
        projectId: String,
        questionId: String,
        request: SubmitChoiceAnswerRequest,
    ): ApiResponse<SubmitChoiceAnswerResponse>

    @Operation(
        summary = "서술형 답변 제출",
        description =
            "쓴 답안을 그대로 저장하고 채점 기준을 받습니다. 채점은 서버가 하지 않고 학습자가 채점 기준을 보고 스스로 합니다. " +
                "같은 문제를 다시 풀면 답이 쌓이지 않고 마지막 것으로 덮어써집니다. 4지선다 문제 id로 이 경로를 부르면 400입니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "제출 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "text가 비었거나 너무 길거나, 4지선다 문제에 서술형으로 답한 경우",
            content = [
                Content(
                    mediaType = APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(name = "필수 값 누락", value = BLANK_TEXT_EXAMPLE),
                        ExampleObject(name = "형식이 다른 문제", value = FORMAT_MISMATCH_EXAMPLE),
                    ],
                ),
            ],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "내 프로젝트가 아니거나, 그 프로젝트의 저장소에 없는 문제",
            content = [
                Content(
                    mediaType = APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(name = "없는 프로젝트", value = PROJECT_NOT_FOUND_EXAMPLE),
                        ExampleObject(name = "없는 문제", value = QUESTION_NOT_FOUND_EXAMPLE),
                    ],
                ),
            ],
        ),
    )
    fun submitEssayAnswer(
        memberId: String,
        projectId: String,
        questionId: String,
        request: SubmitEssayAnswerRequest,
    ): ApiResponse<SubmitEssayAnswerResponse>

    @Operation(
        summary = "문제 북마크",
        description = "문제 풀이 화면 상단 북마크 아이콘 탭 시 북마크 상태를 설정합니다. 토글이 아니라 원하는 상태를 그대로 보냅니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "설정 성공"),
        SwaggerApiResponse(
            responseCode = "400",
            description = "bookmarked가 비어 있음",
            content = [Content(mediaType = APPLICATION_JSON_VALUE, examples = [ExampleObject(value = BOOKMARK_INVALID_INPUT_EXAMPLE)])],
        ),
        SwaggerApiResponse(
            responseCode = "404",
            description = "내 프로젝트가 아니거나, 그 프로젝트의 저장소에 없는 문제",
            content = [
                Content(
                    mediaType = APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(name = "없는 프로젝트", value = PROJECT_NOT_FOUND_EXAMPLE),
                        ExampleObject(name = "없는 문제", value = QUESTION_NOT_FOUND_EXAMPLE),
                    ],
                ),
            ],
        ),
    )
    fun bookmarkQuestion(
        memberId: String,
        projectId: String,
        questionId: String,
        request: BookmarkQuestionRequest,
    ): ApiResponse<BookmarkQuestionResponse>

    companion object {
        // 401은 OpenApiConfig의 loginMemberSecurityCustomizer가 @LoginMember 파라미터를 보고 자동으로 붙이므로 여기 적지 않습니다.
        private const val INVALID_INPUT_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"잘못된 요청입니다","errors":[{"field":"githubRepoUrl","message":"githubRepoUrl은 필수입니다"}]}"""

        // URL 형식이 틀린 것과 GitHub에 없는 것을 구분하지 않습니다. 사용자가 할 일은 어느 쪽이든 URL을 다시 확인하는 것이라 같습니다.
        private const val INVALID_REPOSITORY_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"유효하지 않은 GitHub 저장소입니다","errors":null}"""

        private const val OUT_OF_CHOICE_RANGE_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"선택지 범위를 벗어난 답변입니다","errors":null}"""

        private const val FORMAT_MISMATCH_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"문제 형식과 맞지 않는 답변입니다","errors":null}"""

        private const val BLANK_TEXT_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"잘못된 요청입니다","errors":[{"field":"text","message":"text는 필수입니다"}]}"""

        private const val SUCCESS_EXAMPLE =
            """{"success":true,"data":null,"code":null,"message":null,"errors":null}"""

        private const val PROJECT_LIST_EXAMPLE =
            """{"success":true,"data":{"items":[{"projectId":"68a1f2c3d4e5f6a7b8c9d0e1","repositoryName":"nexters",""" +
                """"repositoryImageUrl":"https://avatars.githubusercontent.com/u/1","techStack":["Kotlin","Compose","Coroutines"],""" +
                """"currentSetLabel":"Set 1","currentSetTitle":"Set 1 title","nextSetId":"set1","nextQuestionId":"q3",""" +
                """"overallProgressPercent":28}],""" +
                """"hasNext":false},"code":null,"message":null,"errors":null}"""

        private const val PROJECT_DETAIL_EXAMPLE =
            """{"success":true,"data":{"projectId":"68a1f2c3d4e5f6a7b8c9d0e1","repositoryUrl":"https://github.com/nexters/nexters",""" +
                """"repositoryName":"nexters","repositoryImageUrl":"https://avatars.githubusercontent.com/u/1","starCount":3600,""" +
                """"techStack":["Kotlin","Compose","Coroutines"],"overallProgressPercent":28,"nextProblemId":"q3",""" +
                """"sets":[{"setId":"set1","label":"Set 1","title":"Set 1 title","problemCount":3,"completedCount":2},""" +
                """{"setId":"set2","label":"Set 2","title":"Set 2 title","problemCount":4,"completedCount":0}]},""" +
                """"code":null,"message":null,"errors":null}"""

        // 남의 프로젝트에도 이 응답을 씁니다. 403으로 답하면 그 id의 프로젝트가 있다는 사실을 알려주는 셈입니다.
        private const val PROJECT_NOT_FOUND_EXAMPLE =
            """{"success":false,"data":null,"code":"PROJECT-001","message":"프로젝트를 찾을 수 없습니다","errors":null}"""

        private const val QUESTION_NOT_FOUND_EXAMPLE =
            """{"success":false,"data":null,"code":"QUIZ-005","message":"문제를 찾을 수 없습니다","errors":null}"""

        private const val LEARNING_SET_NOT_FOUND_EXAMPLE =
            """{"success":false,"data":null,"code":"QUIZ-006","message":"학습 세트를 찾을 수 없습니다","errors":null}"""

        private const val BOOKMARK_INVALID_INPUT_EXAMPLE =
            """{"success":false,"data":null,"code":"COMMON-001","message":"잘못된 요청입니다","errors":[{"field":"bookmarked","message":"bookmarked는 필수입니다"}]}"""
    }
}
