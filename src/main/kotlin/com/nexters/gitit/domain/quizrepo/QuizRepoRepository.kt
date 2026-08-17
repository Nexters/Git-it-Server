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

    /** [ids] 중 살아 있는 것만 돌려줍니다. 없는 id는 결과에서 빠지므로 크기가 [ids]보다 작을 수 있습니다. */
    fun findAllByIds(ids: Collection<String>): List<QuizRepo>

    /**
     * 문제 생성을 기다리는 저장소를 오래 기다린 순서로 돌려줍니다. 대기 중인 것이 없으면 빈 리스트입니다.
     *
     * 대기 중은 [QuizRepoStatus.READY] 하나입니다. 앵커까지 만들고 실패했던 저장소도 [QuizRepo.retry]가
     * READY로 되돌리므로 같은 줄에 섭니다.
     */
    fun findAllPending(): List<QuizRepo>

    fun save(quizRepo: QuizRepo): QuizRepo
}
