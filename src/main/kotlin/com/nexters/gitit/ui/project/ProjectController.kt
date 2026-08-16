package com.nexters.gitit.ui.project

import com.nexters.gitit.application.DeleteProject
import com.nexters.gitit.application.GetProjectDetail
import com.nexters.gitit.application.GetProjects
import com.nexters.gitit.application.RegisterProject
import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.common.LoginMember
import com.nexters.gitit.ui.project.dto.ProjectDetailResponse
import com.nexters.gitit.ui.project.dto.ProjectListResponse
import com.nexters.gitit.ui.project.dto.RegisterProjectRequest
import com.nexters.gitit.ui.project.dto.RegisterProjectResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/projects", produces = [APPLICATION_JSON_VALUE])
class ProjectController(
    private val registerProject: RegisterProject,
    private val getProjects: GetProjects,
    private val getProjectDetail: GetProjectDetail,
    private val deleteProject: DeleteProject,
) : ProjectControllerDocs {
    // 같은 저장소를 다시 등록해도 프로젝트가 새로 생기지 않고 기존 것이 돌아오므로 201이 아닌 200으로 응답합니다.
    @PostMapping
    override fun registerProject(
        @LoginMember memberId: String,
        @Valid @RequestBody request: RegisterProjectRequest,
    ): ApiResponse<RegisterProjectResponse> =
        ApiResponse.success(RegisterProjectResponse.from(registerProject(request.toCommand(memberId))))

    @GetMapping
    override fun getProjects(
        @LoginMember memberId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<ProjectListResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending())
        val result = getProjects(GetProjects.Command(memberId, pageable))
        return ApiResponse.success(ProjectListResponse.from(result))
    }

    @GetMapping("/{projectId}")
    override fun getProjectDetail(
        @LoginMember memberId: String,
        @PathVariable projectId: String,
    ): ApiResponse<ProjectDetailResponse> {
        val result = getProjectDetail(GetProjectDetail.Command(memberId, projectId))
        return ApiResponse.success(ProjectDetailResponse.from(result))
    }

    @DeleteMapping("/{projectId}")
    override fun deleteProject(
        @LoginMember memberId: String,
        @PathVariable projectId: String,
    ): ApiResponse<Unit> {
        deleteProject(DeleteProject.Command(memberId, projectId))
        return ApiResponse.success()
    }
}
