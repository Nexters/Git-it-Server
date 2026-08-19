package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.project.BookmarkQuestion
import io.swagger.v3.oas.annotations.media.Schema

data class BookmarkQuestionResponse(
    @field:Schema(description = "적용된 북마크 상태")
    val bookmarked: Boolean,
) {
    companion object {
        fun from(result: BookmarkQuestion.Result) = BookmarkQuestionResponse(bookmarked = result.bookmarked)
    }
}
