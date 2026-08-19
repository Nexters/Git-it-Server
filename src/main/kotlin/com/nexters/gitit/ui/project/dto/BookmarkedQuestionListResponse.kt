package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.project.GetBookmarkedQuestions
import io.swagger.v3.oas.annotations.media.Schema

data class BookmarkedQuestionListResponse(
    @field:Schema(description = "북마크한 문제 총 개수 (필터 적용 후 기준)")
    val totalCount: Int,
    @field:Schema(description = "북마크가 하나라도 있는 프로젝트 전부. projectId 필터와 무관하게 항상 전체 목록")
    val availableProjects: List<AvailableProjectResponse>,
    val bookmarks: List<BookmarkedQuestionResponse>,
) {
    companion object {
        fun from(result: GetBookmarkedQuestions.Result) =
            BookmarkedQuestionListResponse(
                totalCount = result.totalCount,
                availableProjects = result.availableProjects.map { AvailableProjectResponse(it.projectId, it.projectName) },
                bookmarks = result.bookmarks.map { BookmarkedQuestionResponse.from(it) },
            )
    }
}

data class AvailableProjectResponse(
    val projectId: String,
    val projectName: String,
)

data class BookmarkedQuestionResponse(
    val projectId: String,
    @field:Schema(description = "레포지토리 이름")
    val projectName: String,
    val setId: String,
    @field:Schema(description = "세트 라벨 (예: \"Set 1\")")
    val setLabel: String,
    @field:Schema(description = "세트 내 문제 번호 (1부터 시작)")
    val problemNumber: Int,
    val questionId: String,
    val question: String,
) {
    companion object {
        fun from(item: GetBookmarkedQuestions.BookmarkedQuestion) =
            BookmarkedQuestionResponse(
                projectId = item.projectId,
                projectName = item.projectName,
                setId = item.setId,
                setLabel = item.setLabel,
                problemNumber = item.problemNumber,
                questionId = item.questionId,
                question = item.question,
            )
    }
}
