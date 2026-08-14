package com.nexters.gitit.infrastructure.github

import com.nexters.gitit.domain.quizrepo.GithubRepositoryResolver
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class GithubApiRepositoryResolver(
    private val githubRestClient: RestClient,
) : GithubRepositoryResolver {
    /**
     * 404만 null로 접고 나머지는 그대로 던집니다. 5xx·네트워크 오류까지 "없음"으로 묻으면 멀쩡한 저장소가
     * 등록을 거부당하고, 사용자는 다시 시도하면 되는 상황인 줄 모릅니다.
     */
    override fun resolve(githubRepoUrl: String): String? {
        val (owner, name) = parseOwnerAndName(githubRepoUrl) ?: return null

        return try {
            githubRestClient
                .get()
                .uri("/repos/{owner}/{name}", owner, name)
                .retrieve()
                .body<GithubRepositoryResponse>()
                ?.id
                ?.toString()
        } catch (_: HttpClientErrorException.NotFound) {
            // 비공개 저장소도 404다. 토큰 없이 못 읽는다는 점에서 없는 것과 결과가 같아 구분하지 않는다.
            null
        }
    }

    /** 이름에 점이 들어가는 경우(`socket.io`)와 `.git` 접미사를 모두 받으려고 이름을 최소 일치로 잡습니다. */
    private fun parseOwnerAndName(githubRepoUrl: String): Pair<String, String>? {
        val match = REPO_URL_PATTERN.matchEntire(githubRepoUrl.trim()) ?: return null

        return match.groupValues[1] to match.groupValues[2]
    }

    // 나머지 필드는 Spring Boot 기본 설정(FAIL_ON_UNKNOWN_PROPERTIES=false)이 무시한다.
    private data class GithubRepositoryResponse(
        val id: Long,
    )

    companion object {
        /**
         * 부분 일치로 찾으면 `notgithub.com/o/n`처럼 호스트가 다른 URL도 통과합니다. 호출 대상이 api.github.com으로
         * 고정이라 다른 곳을 찌를 수는 없지만, 사용자가 준 적 없는 저장소가 등록됩니다. 그래서 문자열 전체를 맞춥니다.
         *
         * 소유자·이름을 GitHub 허용 문자로 좁힌 것도 쿼리스트링이 이름에 묻어 들어가지 않게 하려는 것입니다.
         */
        private val REPO_URL_PATTERN = Regex("""(?:https?://)?(?:www\.)?github\.com/([\w.-]+)/([\w.-]+?)(?:\.git)?/?""")
    }
}
