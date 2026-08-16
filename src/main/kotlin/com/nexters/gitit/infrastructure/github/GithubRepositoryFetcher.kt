package com.nexters.gitit.infrastructure.github

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.RepoCheckout
import com.nexters.gitit.domain.quizrepo.RepoCoordinates
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.outputStream

private val logger = KotlinLogging.logger {}

/**
 * gitUrl 을 받아서 이미 받아둔 해제본이 있으면 그것을, 없으면 새로 받아 푼 뒤 [RepoCheckout]을 반환합니다.
 *
 * 디렉터리 이름은 압축을 풀었을 때 나오는 `{owner}-{name}-{sha}` 그대로입니다.
 * 앵커가 어느 커밋 기준인지 나중에 알아야 하므로 sha를 경로에 남깁니다.
 *
 * 주소에서 owner/name만 보므로 `/tree/{branch}` 처럼 뒤에 붙은 경로는 무시되고 항상 default branch를 받습니다.
 */
@Component
class GithubRepositoryFetcher(
    @Value("\${github.work-dir}") workDir: String,
    private val githubClient: HttpClient,
    private val apiBaseUrl: String = API_BASE_URL,
) {
    private val workDir: Path = Path.of(workDir).toAbsolutePath().normalize()

    fun fetch(gitUrl: String): RepoCheckout {
        val match = URL_PATTERN.find(gitUrl.trim()) ?: throw BaseException(ErrorCode.INVALID_REPO_URL, "레포 주소 형식이 아닙니다: $gitUrl")
        val (owner, name) = match.destructured

        // `.`·`..`는 GitHub 이름이 될 수 없는데 URL_PATTERN의 문자 집합은 통과시킨다.
        // 그대로 두면 API 요청 경로에 상위 디렉터리 세그먼트가 실려 엉뚱한 엔드포인트를 부른다.
        if (owner in DOT_SEGMENTS || name in DOT_SEGMENTS) {
            throw BaseException(ErrorCode.INVALID_REPO_URL, "레포 주소 형식이 아닙니다: $gitUrl")
        }

        val root = findDownloaded(owner, name) ?: download(owner, name)

        // GitHub는 사용자가 친 주소가 아니라 정규 표기로 디렉터리 이름을 만들어, 대소문자가 다를 수 있다.
        // 그래서 `{owner}-{name}-` 접두사를 잘라내는 대신 마지막 `-` 뒤를 sha로 본다.
        return RepoCheckout(root, RepoCoordinates(owner, name, root.name.substringAfterLast('-')))
    }

    /**
     * 받기 전에는 sha를 모르므로 정확한 이름 대신 `{owner}-{name}-*` 로 찾습니다.
     * 갱신이 들어와 sha가 여럿 쌓이면 가장 최근 것을 씁니다.
     */
    private fun findDownloaded(
        owner: String,
        name: String,
    ): Path? {
        if (!workDir.exists()) return null

        val prefix = "$owner-$name-"
        return workDir
            .listDirectoryEntries("$prefix*")
            // 접두사 뒤가 sha인지까지 확인한다.
            .filter { SHA_PATTERN.matches(it.name.removePrefix(prefix)) }
            .maxByOrNull { it.getLastModifiedTime() }
    }

    /**
     * 받다가 실패하면 반쯤 풀린 디렉터리가 남는데, 다음 호출이 그걸 완성본으로 착각합니다.
     * 그래서 임시 디렉터리에 푼 뒤 마지막에 옮깁니다.
     */
    private fun download(
        owner: String,
        name: String,
    ): Path {
        val staging = workDir.createDirectories().resolve(".$owner-$name.staging")
        staging.toFile().deleteRecursively()

        try {
            openArchiveStream(owner, name).use { extract(it, staging.createDirectories()) }

            // 아카이브는 내용 전체를 `{owner}-{name}-{sha}` 디렉터리 하나로 감싸고 있다.
            val extracted =
                staging.listDirectoryEntries().singleOrNull() ?: throw BaseException(
                    ErrorCode.INVALID_REPO_ARCHIVE,
                    "아카이브 최상위 디렉터리가 하나가 아닙니다",
                )

            return extracted.moveTo(workDir.resolve(extracted.name))
        } finally {
            staging.toFile().deleteRecursively()
        }
    }

    private fun openArchiveStream(
        owner: String,
        name: String,
    ): InputStream {
        val request =
            HttpRequest
                .newBuilder(URI.create("$apiBaseUrl/repos/$owner/$name/zipball"))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build()

        val response = send(request)
        val status = response.statusCode()
        if (status == HTTP_OK) return response.body()

        response.body().close()
        // 없는 레포와 일시적 실패를 나눈다. 재시도해서 결과가 달라질 수 있는 쪽만 REPO_FETCH_FAILED다.
        throw if (status in INACCESSIBLE_STATUSES) {
            BaseException(ErrorCode.REPO_NOT_ACCESSIBLE, "GitHub 응답 코드: $status")
        } else {
            BaseException(ErrorCode.REPO_FETCH_FAILED, "GitHub 응답 코드: $status")
        }
    }

    private fun send(request: HttpRequest): HttpResponse<InputStream> =
        try {
            githubClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        } catch (e: IOException) {
            logger.warn(e) { "GitHub zipball request failed" }
            throw BaseException(ErrorCode.REPO_FETCH_FAILED, "GitHub 요청에 실패했습니다")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn(e) { "GitHub zipball request interrupted" }
            throw BaseException(ErrorCode.REPO_FETCH_FAILED, "GitHub 요청이 중단되었습니다")
        }

    private fun extract(
        source: InputStream,
        destination: Path,
    ) {
        try {
            ZipInputStream(source).use { zip ->
                generateSequence { zip.nextEntry }.forEach { writeEntry(zip, it, destination) }
            }
        } catch (e: IOException) {
            logger.warn(e) { "GitHub zipball extraction failed" }
            throw BaseException(ErrorCode.INVALID_REPO_ARCHIVE, "아카이브를 해제하지 못했습니다")
        }
    }

    private fun writeEntry(
        source: InputStream,
        entry: ZipEntry,
        destination: Path,
    ) {
        val target = destination.resolve(entry.name).normalize()

        // 임의의 레포에서 받은 아카이브라 내용을 신뢰하지 않는다. `../`가 섞이면 목적지 밖에 파일을 쓰게 된다.
        if (!target.startsWith(destination)) {
            throw BaseException(ErrorCode.INVALID_REPO_ARCHIVE, "대상 디렉터리를 벗어나는 경로가 있습니다: ${entry.name}")
        }

        if (entry.isDirectory) {
            target.createDirectories()
            return
        }

        target.parent.createDirectories()
        target.outputStream().use { source.copyTo(it) }
    }

    companion object {
        // 브라우저에서 복사한 주소에는 /tree/main 같은 꼬리가 붙어 있어 뒤 경로를 받아주고 버린다.
        private val URL_PATTERN = Regex("""^(?:https?://)?(?:www\.)?github\.com/([\w.-]+)/([\w.-]+?)(?:\.git)?(?:/.*)?$""")

        private val DOT_SEGMENTS = setOf(".", "..")

        // zipball이 감싼 디렉터리 이름 끝에 붙는 축약 sha. 길이는 레포마다 달라 범위로 받는다.
        private val SHA_PATTERN = Regex("[0-9a-f]{7,40}")
        private const val API_BASE_URL = "https://api.github.com"
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val HTTP_OK = 200
        private val INACCESSIBLE_STATUSES = setOf(401, 403, 404)
        private val REQUEST_TIMEOUT = Duration.ofMinutes(5)
    }
}
