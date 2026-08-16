package com.nexters.gitit.domain.quizrepo

interface GithubRepositoryResolver {
    /**
     * URL을 파싱하고 GitHub에 실재하는지 확인해 그 저장소의 GitHub id를 반환합니다. 둘을 한 호출로 묶은 것은
     * id를 얻었다는 사실 자체가 곧 등록 가능하다는 뜻이 되게 하려는 것입니다. 파싱 실패든 GitHub에 없음이든
     * 구분 없이 null이라, 호출부는 "파싱은 됐는데 없는 저장소" 같은 중간 상태를 다루지 않습니다.
     *
     * `owner/name`이 아니라 id인 것은 리네임·소유자 이전을 견디기 위해서입니다. 이름을 키로 잡으면 리네임
     * 한 번에 같은 저장소의 문제 세트가 둘로 갈라집니다.
     */
    fun resolve(githubRepoUrl: String): String?
}
