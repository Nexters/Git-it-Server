package com.nexters.gitit.infrastructure.github

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.quizrepo.RepoCoordinates
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isWritable
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.setPosixFilePermissions

class GithubRepositoryFetcherTest {
    @TempDir
    lateinit var workDir: Path

    private val githubClient: HttpClient = mock()

    @Test
    fun `아카이브를 받아 owner-name-sha 디렉터리로 푼다`() {
        respondWith(
            OK,
            zipOf(
                "$ARCHIVE_ROOT/" to null,
                "$ARCHIVE_ROOT/README.md" to README,
                "$ARCHIVE_ROOT/src/Main.kt" to MAIN,
            ),
        )

        val checkout = fetcher().fetch(REPO_URL)

        checkout.root shouldBe workDir.resolve(ARCHIVE_ROOT)
        checkout.repo shouldBe RepoCoordinates("Nexters", "Git-it-Server", "abc1234")
        checkout.root.resolve("README.md").readText() shouldBe README
        checkout.root.resolve("src/Main.kt").readText() shouldBe MAIN
    }

    @Test
    fun `받은 뒤 같은 주소를 다시 부르면 요청하지 않는다`() {
        respondWith(OK, zipOf("$ARCHIVE_ROOT/" to null, "$ARCHIVE_ROOT/README.md" to README))

        val downloaded = fetch(REPO_URL)

        fetch(REPO_URL) shouldBe downloaded
        verify(githubClient, times(1)).send(any(), any<HttpResponse.BodyHandler<InputStream>>())
    }

    @Test
    fun `여러 형태의 github 주소에서 owner와 name을 뽑는다`() {
        // 이미 받아둔 상태로 만들어 파싱 결과만 드러나게 한다.
        val downloaded = workDir.resolve(ARCHIVE_ROOT).createDirectories()

        listOf(
            "https://github.com/Nexters/Git-it-Server",
            "https://github.com/Nexters/Git-it-Server/tree/main",
            "https://github.com/Nexters/Git-it-Server/blob/main/README.md",
            "https://github.com/Nexters/Git-it-Server.git",
            "github.com/Nexters/Git-it-Server",
            "  https://www.github.com/Nexters/Git-it-Server  ",
        ).forAll { fetch(it) shouldBe downloaded }

        verify(githubClient, never()).send(any(), any<HttpResponse.BodyHandler<InputStream>>())
    }

    @Test
    fun `github 주소가 아니면 INVALID_REPO_URL을 던진다`() {
        listOf(
            "https://gitlab.com/Nexters/Git-it-Server",
            "https://github.com/Nexters",
            "",
            // 문자 집합만 보면 통과하지만 API 요청 경로에 상위 디렉터리 세그먼트가 실린다.
            "https://github.com/../..",
        ).forAll { shouldThrowErrorCode(ErrorCode.INVALID_REPO_URL) { fetcher().fetch(it) } }
    }

    @Test
    fun `404는 REPO_NOT_ACCESSIBLE이다`() {
        respondWith(NOT_FOUND, ByteArray(0))

        shouldThrowErrorCode(ErrorCode.REPO_NOT_ACCESSIBLE) { fetcher().fetch(REPO_URL) }
    }

    @Test
    fun `429는 REPO_FETCH_FAILED다`() {
        respondWith(TOO_MANY_REQUESTS, ByteArray(0))

        // 이 코드가 GenerateQuiz에서 재시도 가능한 실패(FAILED)로 갈리는 근거다. 접근 불가로 새면 REJECTED로 굳는다.
        shouldThrowErrorCode(ErrorCode.REPO_FETCH_FAILED) { fetcher().fetch(REPO_URL) }
    }

    @Test
    fun `쿼터가 바닥난 403은 REPO_FETCH_FAILED다`() {
        respondWith(FORBIDDEN, ByteArray(0), mapOf("x-ratelimit-remaining" to listOf("0")))

        shouldThrowErrorCode(ErrorCode.REPO_FETCH_FAILED) { fetcher().fetch(REPO_URL) }
    }

    @Test
    fun `쿼터가 남은 403은 REPO_NOT_ACCESSIBLE이다`() {
        respondWith(FORBIDDEN, ByteArray(0), mapOf("x-ratelimit-remaining" to listOf("42")))

        // 헤더를 안 보고 403을 통째로 일시적 실패로 읽으면, 막힌 레포가 영영 대기줄을 돈다.
        shouldThrowErrorCode(ErrorCode.REPO_NOT_ACCESSIBLE) { fetcher().fetch(REPO_URL) }
    }

    @Test
    fun `목적지를 벗어나는 엔트리는 거절한다`() {
        respondWith(OK, zipOf("$ARCHIVE_ROOT/" to null, "$ARCHIVE_ROOT/../../evil.txt" to "evil"))

        shouldThrowErrorCode(ErrorCode.INVALID_REPO_ARCHIVE) { fetcher().fetch(REPO_URL) }

        workDir.resolve("evil.txt").exists().shouldBeFalse()
    }

    @Test
    fun `쓸 수 없는 작업 공간이면 만들어지지도 않는다`() {
        val readOnly = workDir.resolve("read-only").createDirectories()
        readOnly.setPosixFilePermissions(PosixFilePermissions.fromString("r-xr-xr-x"))
        // root는 권한을 무시하고 쓰므로 검사 자체가 성립하지 않는다.
        assumeTrue(!readOnly.isWritable(), "root로 도는 환경에서는 확인할 수 없다")

        // 잘못된 경로를 받고도 살아 있으면, 생성 요청이 들어올 때까지 문제가 숨는다.
        shouldThrow<IllegalArgumentException> { GithubRepositoryFetcher(readOnly.toString(), githubClient, "") }
    }

    @Test
    @Tag("network")
    fun `실제 공개 레포를 받아 푼다`() {
        val fetcher = GithubRepositoryFetcher(workDir.toString(), GithubHttpClientConfiguration().githubClient(), githubToken())

        val checkout = fetcher.fetch(REPO_URL)
        val root = checkout.root

        root.name shouldStartWith "Nexters-Git-it-Server-"
        // 실제 응답에서만 드러나는 부분이다. 축약 sha 길이가 레포마다 달라 값이 아니라 꼴로 본다.
        checkout.repo.sha shouldMatch Regex("[0-9a-f]{7,40}")
        root.resolve("README.md").exists().shouldBeTrue()
    }

    @Test
    fun `토큰이 있으면 인증 헤더를 실어 보낸다`() {
        respondWith(OK, zipOf("$ARCHIVE_ROOT/" to null, "$ARCHIVE_ROOT/README.md" to README))

        fetcher(token = "gh-token").fetch(REPO_URL)

        val request = argumentCaptor<HttpRequest>()
        verify(githubClient).send(request.capture(), any<HttpResponse.BodyHandler<InputStream>>())
        val headers = request.firstValue.headers()

        headers.firstValue("Authorization").orElse(null) shouldBe "Bearer gh-token"
    }

    // 이 테스트들은 해제 위치만 보므로 좌표까지 매번 풀어 쓰지 않는다.
    private fun fetch(gitUrl: String): Path {
        val checkout = fetcher().fetch(gitUrl)

        return checkout.root
    }

    private fun fetcher(token: String = "") =
        GithubRepositoryFetcher(
            workDir = workDir.toString(),
            githubClient = githubClient,
            token = token,
        )

    private fun respondWith(
        status: Int,
        body: ByteArray,
        headers: Map<String, List<String>> = emptyMap(),
    ) {
        val response =
            mock<HttpResponse<InputStream>> {
                on { statusCode() } doReturn status
                on { body() } doReturn ByteArrayInputStream(body)
                on { headers() } doReturn HttpHeaders.of(headers) { _, _ -> true }
            }

        whenever(githubClient.send(any(), any<HttpResponse.BodyHandler<InputStream>>())) doReturn response
    }

    /**
     * content가 null이면 디렉터리 엔트리입니다. 이름이 `/`로 끝나야 [ZipEntry.isDirectory]가 참이 됩니다.
     */
    private fun zipOf(vararg entries: Pair<String, String?>): ByteArray {
        val buffer = ByteArrayOutputStream()

        ZipOutputStream(buffer).use { zip ->
            entries.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                content?.let { zip.write(it.toByteArray()) }
                zip.closeEntry()
            }
        }

        return buffer.toByteArray()
    }

    private fun shouldThrowErrorCode(
        expected: ErrorCode,
        block: () -> Unit,
    ) {
        shouldThrow<BaseException>(block).errorCode shouldBe expected
    }

    companion object {
        private const val REPO_URL = "https://github.com/Nexters/Git-it-Server"

        // zipball이 내용 전체를 감싸는 디렉터리. sha 자리는 16진수여야 캐시 탐색에 걸린다.
        private const val ARCHIVE_ROOT = "Nexters-Git-it-Server-abc1234"
        private const val README = "# Git-it"
        private const val MAIN = "fun main() {}"
        private const val OK = 200
        private const val NOT_FOUND = 404
        private const val FORBIDDEN = 403
        private const val TOO_MANY_REQUESTS = 429
    }
}
