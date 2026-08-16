package com.nexters.gitit.domain.quizrepo

/**
 * [id]만 식별용이고 나머지는 보여주기 위한 값입니다. 한 타입에 담은 것은 어차피 같은 응답에서 함께 나오기
 * 때문입니다 — 따로 받으러 가면 등록 한 번에 GitHub을 두 번 찌르게 됩니다.
 */
data class GithubRepository(
    val id: String,
    val name: String,
    val ownerImageUrl: String,
    val starCount: Int,
    val techStacks: List<String>,
)
