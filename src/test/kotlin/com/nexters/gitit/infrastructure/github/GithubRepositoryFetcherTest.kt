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
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
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
import java.net.http.HttpResponse
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText

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
        ).forAll { shouldThrowErrorCode(ErrorCode.INVALID_REPO_URL) { fetcher().fetch(it) } }
    }

    @Test
    fun `404는 REPO_NOT_ACCESSIBLE이다`() {
        respondWith(NOT_FOUND, ByteArray(0))

        shouldThrowErrorCode(ErrorCode.REPO_NOT_ACCESSIBLE) { fetcher().fetch(REPO_URL) }
    }

    @Test
    fun `목적지를 벗어나는 엔트리는 거절한다`() {
        respondWith(OK, zipOf("$ARCHIVE_ROOT/" to null, "$ARCHIVE_ROOT/../../evil.txt" to "evil"))

        shouldThrowErrorCode(ErrorCode.INVALID_REPO_ARCHIVE) { fetcher().fetch(REPO_URL) }

        workDir.resolve("evil.txt").exists().shouldBeFalse()
    }

    @Test
    @Tag("network")
    fun `실제 공개 레포를 받아 푼다`() {
        val fetcher = GithubRepositoryFetcher(workDir.toString(), GithubHttpClientConfiguration().githubClient())

        val checkout = fetcher.fetch(REPO_URL)
        val root = checkout.root

        root.name shouldStartWith "Nexters-Git-it-Server-"
        // 실제 응답에서만 드러나는 부분이다. 축약 sha 길이가 레포마다 달라 값이 아니라 꼴로 본다.
        checkout.repo.sha shouldMatch Regex("[0-9a-f]{7,40}")
        root.resolve("README.md").exists().shouldBeTrue()
    }

    // 이 테스트들은 해제 위치만 보므로 좌표까지 매번 풀어 쓰지 않는다.
    private fun fetch(gitUrl: String): Path {
        val checkout = fetcher().fetch(gitUrl)

        return checkout.root
    }

    private fun fetcher() =
        GithubRepositoryFetcher(
            workDir = workDir.toString(),
            githubClient = githubClient,
        )

    private fun respondWith(
        status: Int,
        body: ByteArray,
    ) {
        val response =
            mock<HttpResponse<InputStream>> {
                on { statusCode() } doReturn status
                on { body() } doReturn ByteArrayInputStream(body)
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
    }
}
