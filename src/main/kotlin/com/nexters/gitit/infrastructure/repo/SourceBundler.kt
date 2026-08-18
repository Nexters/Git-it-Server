package com.nexters.gitit.infrastructure.repo

import com.nexters.gitit.domain.quizrepo.Anchor
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

private val logger = KotlinLogging.logger {}

/**
 * 개념 하나의 후보 파일들을 라인 번호가 붙은 묶음 하나로 만듭니다.
 *
 * 심볼 인덱스가 없어 앵커를 "찾아내라"가 아니라 "여기서 골라라"로 묻습니다.
 * LLM은 준 것 중에서만 고르므로 무엇을 주느냐가 곧 앵커 품질입니다.
 *
 * 캡은 전부 코드로 강제합니다. "300라인만 봐라"를 프롬프트에 적는 것은 캡이 아닙니다 —
 * 부탁은 지켜지지 않을 때가 있고, 그때 폭발하는 것은 콜당 입력 토큰입니다.
 */
@Component
class SourceBundler {
    fun bundle(
        repoRoot: Path,
        paths: List<String>,
    ): String =
        paths
            .take(MAX_FILES)
            .mapNotNull { entry(repoRoot, it) }
            .joinToString("\n\n")

    /**
     * 소스 파일 한 개를 줄 단위로 읽습니다. 읽을 수 없는 파일은 null입니다.
     *
     * 앵커 검증도 같은 파일을 다시 읽어야 하는데, 그쪽만 캡 없이 읽으면
     * 번들이 걸러낸 바이너리·거대 파일이 그 경로로 되살아납니다.
     */
    fun lines(
        repoRoot: Path,
        path: String,
    ): List<String>? = readText(repoRoot.resolve(path), path)?.removePrefix(BOM)?.lines()

    /**
     * 확정된 앵커의 본문만 번호를 붙여 발췌합니다. 문제 생성 콜이 보는 코드는 이것뿐입니다.
     *
     * 앵커에 1..N 번호를 매기면 "몇 번 앵커를 근거로 냈는지"의 검증이 범위 비교 한 번으로 끝나고,
     * 없는 앵커를 지어낼 여지가 없습니다.
     */
    fun excerpt(
        repoRoot: Path,
        anchors: List<Anchor>,
    ): String =
        anchors
            .mapIndexed { index, anchor -> excerpt(repoRoot, index + 1, anchor) }
            .joinToString("\n\n")

    private fun excerpt(
        repoRoot: Path,
        number: Int,
        anchor: Anchor,
    ): String {
        val header = "[코드 $number] ${anchor.file}:${anchor.startLine}-${anchor.endLine} (${anchor.kind})"
        val lines = lines(repoRoot, anchor.file) ?: return header

        // 앵커 범위는 이미 검증된 값이지만, 파일이 그사이 바뀌었을 수 있어 다시 자른다.
        val from = (anchor.startLine - 1).coerceIn(0, lines.size)
        val to = anchor.endLine.coerceIn(from, lines.size)
        val body = lines.subList(from, to).mapIndexed { index, line -> numbered(from + index + 1, line) }

        return (listOf(header) + body).joinToString("\n")
    }

    private fun entry(
        repoRoot: Path,
        path: String,
    ): String? {
        val lines = lines(repoRoot, path) ?: return null

        // 전체 줄 수를 함께 알려야 잘린 파일이라는 것이 드러난다.
        return "--- $path (총 ${lines.size}줄) ---\n" + numbered(lines)
    }

    /**
     * 라인 번호는 파일의 실제 번호를 그대로 씁니다. 잘라낸 뒤에도 다시 매기지 않습니다.
     * 콜이 돌려준 번호를 그 자리에서 대조하는 것이 앵커 검증의 전부이기 때문입니다.
     *
     * 긴 파일은 앞뒤를 남기고 가운데를 버립니다. 앞에서 통째로 자르면 import·필드·생성자만 남아
     * 앵커가 선언부에만 몰립니다 — LLM은 준 것 중에서만 고릅니다.
     */
    private fun numbered(lines: List<String>): String {
        if (lines.size <= MAX_LINES) {
            return lines.mapIndexed { index, line -> numbered(index + 1, line) }.joinToString("\n")
        }

        val tailStart = lines.size - MAX_LINES / 2
        val head = lines.take(MAX_LINES / 2).mapIndexed { index, line -> numbered(index + 1, line) }
        val tail = lines.drop(tailStart).mapIndexed { index, line -> numbered(tailStart + index + 1, line) }

        return (head + "... (${tailStart - MAX_LINES / 2}줄 생략) ..." + tail).joinToString("\n")
    }

    // 줄 하나가 파일 전체인 minified 파일이 예산을 통째로 먹지 못하게 막는다.
    private fun numbered(
        number: Int,
        line: String,
    ) = "${number.toString().padStart(LINE_NUMBER_WIDTH)}| ${line.take(LINE_CHAR_CAP)}"

    /**
     * 후보 경로에는 확장자 화이트리스트가 없어 이미지나 아카이브가 섞일 수 있습니다.
     * 이름만 보고 거르는 대신 UTF-8로 읽히는지 확인해 아닌 것은 건너뜁니다.
     */
    private fun readText(
        file: Path,
        relative: String,
    ): String? =
        try {
            // 라인 캡이 있어도 파일 전체를 메모리에 올리는 것은 막아야 한다.
            if (!file.isRegularFile() || file.fileSize() > MAX_FILE_BYTES) {
                logger.debug { "Skipped unusable source: $relative" }
                null
            } else {
                StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file.readBytes()))
                    .toString()
            }
        } catch (e: CharacterCodingException) {
            logger.debug(e) { "Skipped non-UTF-8 source: $relative" }
            null
        } catch (e: IOException) {
            logger.debug(e) { "Skipped unreadable source: $relative" }
            null
        }

    companion object {
        private const val MAX_FILES = 5
        private const val MAX_LINES = 300
        private const val LINE_CHAR_CAP = 400
        private const val LINE_NUMBER_WIDTH = 5
        private const val MAX_FILE_BYTES = 1L * 1024 * 1024
        private const val BOM = "﻿"
    }
}
