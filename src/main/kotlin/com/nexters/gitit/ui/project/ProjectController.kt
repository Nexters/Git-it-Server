package com.nexters.gitit.ui.project

import com.nexters.gitit.application.RegisterProject
import com.nexters.gitit.ui.common.ApiResponse
import com.nexters.gitit.ui.common.LoginMember
import com.nexters.gitit.ui.project.dto.RegisterProjectRequest
import com.nexters.gitit.ui.project.dto.RegisterProjectResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/projects", produces = [APPLICATION_JSON_VALUE])
class ProjectController(
    private val registerProject: RegisterProject,
) : ProjectControllerDocs {
    // 같은 저장소를 다시 등록해도 프로젝트가 새로 생기지 않고 기존 것이 돌아오므로 201이 아닌 200으로 응답합니다.
    @PostMapping
    override fun registerProject(
        @LoginMember memberId: String,
        @Valid @RequestBody request: RegisterProjectRequest,
    ): ApiResponse<RegisterProjectResponse> =
        ApiResponse.success(RegisterProjectResponse.from(registerProject(request.toCommand(memberId))))
}
