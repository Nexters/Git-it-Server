package com.nexters.gitit.domain.quizrepo

interface QuizRepoRepository {
    /**
     * 같은 저장소가 이미 있으면 그것을, 없으면 새로 저장한 것을 돌려줍니다.
     *
     * 여러 요청이 같은 저장소를 동시에 등록해도 하나만 남습니다. 그래서 호출부는 넘긴 객체가 그대로 돌아왔는지로
     * 자기가 만든 것인지 알 수 있습니다.
     */
    fun saveIfAbsent(quizRepo: QuizRepo): QuizRepo

    fun findById(id: String): QuizRepo?

    fun save(quizRepo: QuizRepo): QuizRepo
}
