package com.nexters.gitit.infrastructure.github

import com.fasterxml.jackson.annotation.JsonProperty
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.GithubRepository
import com.nexters.gitit.domain.quizrepo.GithubRepositoryResolver
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

private val logger = KotlinLogging.logger {}

@Component
class GithubApiRepositoryResolver(
    private val githubRestClient: RestClient,
) : GithubRepositoryResolver {
    /**
     * 404만 null로 접고 나머지는 그대로 던집니다. 5xx·네트워크 오류까지 "없음"으로 묻으면 멀쩡한 저장소가
     * 등록을 거부당하고, 사용자는 다시 시도하면 되는 상황인 줄 모릅니다.
     *
     * 쿼터 초과(429)만 예외로 갈아 끼웁니다. 그대로 두면 최종 방어선이 500 + 스택트레이스로 내보내
     * 서버 사고와 구분되지 않는데, 실제로는 잠시 뒤 다시 등록하면 되는 상황입니다.
     */
    override fun resolve(githubRepoUrl: String): GithubRepository? {
        val (owner, name) = parseOwnerAndName(githubRepoUrl) ?: return null

        val response =
            try {
                githubRestClient
                    .get()
                    .uri("/repos/{owner}/{name}", owner, name)
                    .retrieve()
                    .body<GithubRepositoryResponse>()
            } catch (_: HttpClientErrorException.NotFound) {
                // 비공개 저장소도 404다. 토큰 없이 못 읽는다는 점에서 없는 것과 결과가 같아 구분하지 않는다.
                null
            } catch (e: HttpClientErrorException) {
                if (!e.isRateLimited()) throw e

                // BaseException은 핸들러가 로깅하지 않아, 원인은 여기서 남긴다.
                logger.warn(e) { "GitHub rate limit hit while resolving $githubRepoUrl" }
                throw BaseException(ErrorCode.REPO_FETCH_FAILED, "GitHub 요청이 잠시 제한됐습니다. 잠시 후 다시 시도해 주세요")
            } ?: return null

        return GithubRepository(
            id = response.id.toString(),
            name = response.name,
            ownerImageUrl = response.owner.avatarUrl,
            starCount = response.stargazersCount,
            // 언어 통계는 별도 호출인 데다 `HTML`·`Shell` 같은 곁다리가 올라와, 안 채워지면 안 채워진 대로 둔다.
            techStacks = response.topics.take(TECH_STACK_SIZE),
        )
    }

    /**
     * 주 쿼터가 바닥나면 GitHub은 429가 아니라 403으로 답합니다. 상태 코드만으로는 접근 권한이 없는 레포와
     * 구분되지 않아 남은 횟수 헤더를 봅니다 — 2차 제한은 `Retry-After`로 옵니다.
     */
    private fun HttpClientErrorException.isRateLimited(): Boolean =
        statusCode == HttpStatus.TOO_MANY_REQUESTS ||
            (
                statusCode == HttpStatus.FORBIDDEN &&
                    (responseHeaders?.getFirst("retry-after") != null || responseHeaders?.getFirst("x-ratelimit-remaining") == "0")
            )

    /** 이름에 점이 들어가는 경우(`socket.io`)와 `.git` 접미사를 모두 받으려고 이름을 최소 일치로 잡습니다. */
    private fun parseOwnerAndName(githubRepoUrl: String): Pair<String, String>? {
        val match = REPO_URL_PATTERN.matchEntire(githubRepoUrl.trim()) ?: return null

        return match.groupValues[1] to match.groupValues[2]
    }

    // 나머지 필드는 Spring Boot 기본 설정(FAIL_ON_UNKNOWN_PROPERTIES=false)이 무시한다.
    // 프로퍼티 이름 전략은 기본값(camelCase)이라 snake_case 필드는 @JsonProperty로 짚어줘야 붙는다.
    private data class GithubRepositoryResponse(
        val id: Long,
        val name: String,
        val owner: Owner,
        @param:JsonProperty("stargazers_count") val stargazersCount: Int,
        // topics는 응답에 아예 없는 경우가 있어 기본값을 둔다.
        val topics: List<String> = emptyList(),
    ) {
        data class Owner(
            @param:JsonProperty("avatar_url") val avatarUrl: String,
        )
    }

    companion object {
        private const val TECH_STACK_SIZE = 3

        /**
         * 부분 일치로 찾으면 `notgithub.com/o/n`처럼 호스트가 다른 URL도 통과합니다. 호출 대상이 api.github.com으로
         * 고정이라 다른 곳을 찌를 수는 없지만, 사용자가 준 적 없는 저장소가 등록됩니다. 그래서 문자열 전체를 맞춥니다.
         *
         * 소유자·이름을 GitHub 허용 문자로 좁힌 것도 쿼리스트링이 이름에 묻어 들어가지 않게 하려는 것입니다.
         */
        private val REPO_URL_PATTERN = Regex("""(?:https?://)?(?:www\.)?github\.com/([\w.-]+)/([\w.-]+?)(?:\.git)?/?""")
    }
}
