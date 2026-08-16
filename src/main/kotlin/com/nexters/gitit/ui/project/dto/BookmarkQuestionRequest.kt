package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.BookmarkQuestion
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class BookmarkQuestionRequest(
    @field:Schema(description = "북마크 설정 여부")
    @field:NotNull(message = "bookmarked는 필수입니다")
    val bookmarked: Boolean?,
) {
    fun toCommand(
        memberId: String,
        projectId: String,
        questionId: String,
    ) = BookmarkQuestion.Command(
        memberId = memberId,
        projectId = projectId,
        questionId = questionId,
        bookmarked = bookmarked!!,
    )
}
