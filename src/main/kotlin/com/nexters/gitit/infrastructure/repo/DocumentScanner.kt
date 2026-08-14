package com.nexters.gitit.infrastructure.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readBytes

private val logger = KotlinLogging.logger {}

/**
 * 레포 순회 결과. 두 목록 모두 레포 루트 기준 상대 경로이고, 읽는 쪽이 레포 루트에 resolve 합니다.
 *
 * 절대 경로는 산출물에 실리면 특정 머신의 해제 위치에 묶여 재실행이 깨지고,
 * 로그나 예외 메시지를 타고 나가면 서버 디렉터리 구조를 노출합니다.
 */
data class RepositoryFiles(
    val documents: List<String>,
    val sources: List<String>,
)

/**
 * 해제된 레포를 한 번 순회해 문서와 나머지 파일을 가릅니다.
 *
 * 여기서 '문서'는 확장자가 `md`·`rst`·`adoc`이면서 번역본·생성된 API 레퍼런스·이력 문서가
 * 아닌 파일이고, 나머지는 전부 소스입니다 — 소스 쪽에 확장자 화이트리스트를 두면 언어마다
 * 유지보수가 생기는데, 조회 인덱스는 경로의 실재 여부만 답하면 되므로 언어를 알 필요가 없습니다.
 *
 * 두 목록 모두 사전순입니다. 파일시스템 순회 순서에 기대면 같은 레포에서도 목록이 달라집니다.
 */
@Component
class DocumentScanner {
    fun scan(repoRoot: Path): RepositoryFiles {
        if (!repoRoot.isDirectory()) return RepositoryFiles(emptyList(), emptyList())

        val documents = mutableListOf<String>()
        val sources = mutableListOf<String>()
        // 심볼릭 링크를 따라가지 않는다. 임의의 레포에는 자기 조상을 가리키는 링크가 있고 그러면 순회가 끝나지 않는다.
        Files.walkFileTree(repoRoot, Collector(repoRoot, documents, sources))

        return RepositoryFiles(documents = documents.sorted(), sources = sources.sorted())
    }

    private inner class Collector(
        private val repoRoot: Path,
        private val documents: MutableList<String>,
        private val sources: MutableList<String>,
    ) : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes,
        ): FileVisitResult {
            // 훑고 버리는 대신 진입 자체를 막는다. node_modules 하나가 레포 전체보다 클 수 있다.
            val excluded = dir != repoRoot && dir.name.lowercase() in EXCLUDED_DIRECTORIES
            return if (excluded) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
        }

        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes,
        ): FileVisitResult {
            if (attrs.isRegularFile) {
                val relative = repoRoot.relativize(file).invariantSeparatorsPathString
                if (isDocument(file, relative)) documents.add(relative) else sources.add(relative)
            }
            return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed(
            file: Path,
            exc: IOException,
        ): FileVisitResult {
            logger.debug(exc) { "Skipped unreadable path: ${relativize(file)}" }
            return FileVisitResult.CONTINUE
        }

        // 로그에도 절대 경로를 남기지 않는다. 로그 수집기로 나가면 서버 디렉터리 구조가 새어나간다.
        private fun relativize(file: Path) =
            runCatching { repoRoot.relativize(file).invariantSeparatorsPathString }.getOrDefault(file.fileName.toString())
    }

    private fun isDocument(
        file: Path,
        relative: String,
    ): Boolean =
        file.extension.lowercase() in DOCUMENT_EXTENSIONS &&
            !isTranslation(file.name) &&
            EXCLUDED_FILE_PREFIXES.none { file.name.uppercase().startsWith(it) } &&
            isUtf8(file, relative)

    /**
     * `README.zh-CN.md`처럼 확장자 앞에 로케일이 붙은 번역본을 걸러냅니다.
     * 로케일은 고정 목록입니다. 확장자 앞 토큰을 전부 로케일로 보면 `spec.v2.md` 같은 멀쩡한 문서가 사라집니다.
     */
    private fun isTranslation(fileName: String): Boolean {
        val beforeExtension = fileName.substringBeforeLast('.', "")
        return beforeExtension.substringAfterLast('.', "").lowercase() in LOCALES
    }

    private fun isUtf8(
        file: Path,
        relative: String,
    ): Boolean =
        try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(file.readBytes()))
            true
        } catch (e: CharacterCodingException) {
            logger.debug(e) { "Skipped non-UTF-8 document: $relative" }
            false
        } catch (e: IOException) {
            logger.debug(e) { "Skipped unreadable document: $relative" }
            false
        }

    companion object {
        private val DOCUMENT_EXTENSIONS = setOf("md", "rst", "adoc")

        private val LOCALES = setOf("ja", "zh", "ko", "fr", "de", "es", "pt", "ru", "zh-cn", "zh-tw", "pt-br")

        private val EXCLUDED_DIRECTORIES =
            setOf(
                // 번역본 — 같은 내용이 언어 수만큼 중복된다.
                "i18n",
                "locales",
                "translations",
                // 생성된 API 레퍼런스 — 시그니처 나열이라 설계 의도가 없다.
                "javadoc",
                "dokka",
                "apidocs",
                "_build",
                "site",
                // 남의 코드.
                "third_party",
                "vendor",
                "node_modules",
                ".git",
                ".github",
            ) + LOCALES

        // 이력·거버넌스 문서. 무엇을 왜 그렇게 만들었는지를 말하지 않는다.
        private val EXCLUDED_FILE_PREFIXES =
            setOf("CHANGELOG", "RELEASE_NOTES", "RELEASE-NOTES", "CONTRIBUTING", "CODE_OF_CONDUCT", "SECURITY.MD")
    }
}
