package com.nexters.gitit.infrastructure.repo

import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * 랭킹 순서대로 예산을 채워, 개념 추출 콜에 넘길 문서 묶음 하나를 만듭니다.
 *
 * 남은 예산에 전문이 들어가면 전문을, 아니면 헤딩만 넣습니다.
 * 이 뺄셈 하나가 레포 크기 분기를 대신합니다 — 작은 레포는 루프 내내 예산이 남아
 * 전부 전문으로 들어가고, 큰 레포는 중간부터 자동으로 "핵심 전문 + 나머지 목차"가 됩니다.
 * 덕분에 "크면 이렇게, 작으면 저렇게"라는 판단이 코드에 없습니다.
 */
@Component
class DocumentBundler {
    fun bundle(
        repoRoot: Path,
        ranked: List<String>,
    ): String {
        val builder = StringBuilder()
        var remaining = BUDGET_CHARS

        for (document in ranked) {
            if (remaining <= 0) break

            val text = repoRoot.resolve(document).readText().removePrefix(BOM)

            // 문서 하나가 예산을 통째로 먹지 못하도록 파일당 상한을 따로 둔다.
            val fullText = text.length <= PER_DOCUMENT_CAP_CHARS && text.length <= remaining
            val body = if (fullText) text else outline(document, text)
            val entry = header(document, fullText) + body

            // 들어가지 않는 문서는 건너뛰기만 한다. 뒤에 더 짧은 문서가 남아 있을 수 있다.
            if (entry.length <= remaining) {
                builder.append(entry).append("\n\n")
                remaining -= entry.length
            }
        }

        return builder.toString().trimEnd()
    }

    /**
     * 머리말은 전문이든 헤딩만이든 항상 붙입니다.
     * 콜이 근거 문서를 경로로 지목할 수 있어야 나중의 원문 대조가 성립하기 때문입니다.
     *
     * `[headings only]` 표시가 빠지면 목차만 받고도 다 읽은 줄 알고 본문 내용을 지어냅니다.
     */
    private fun header(
        document: String,
        fullText: Boolean,
    ) = if (fullText) "--- $document ---\n" else "--- $document [headings only] ---\n"

    private fun outline(
        document: String,
        text: String,
    ): String {
        // reStructuredText는 제목 밑줄이 별도 줄에 오는 형식이라 헤딩만 추리기 어렵다. 앞부분으로 대신한다.
        if (document.endsWith(".rst")) {
            return text.lineSequence().take(OUTLINE_FALLBACK_LINES).joinToString("\n")
        }

        return headings(text)
    }

    /**
     * 코드 블록 안의 줄은 건너뛰고 제목 줄만 모읍니다.
     *
     * 블록 안에서 걸러내지 않으면 예제로 실린 셸 스크립트의 `# install deps` 같은 주석이
     * 제목으로 잡혀, 목차만 받은 쪽이 있지도 않은 절이 있다고 읽습니다.
     */
    private fun headings(text: String): String {
        val headings = mutableListOf<String>()
        var openFence: String? = null

        for (line in text.lineSequence()) {
            val fence = FENCE.find(line)?.groupValues?.get(1)
            if (fence != null) {
                openFence = toggle(openFence, fence)
                continue
            }
            if (openFence == null && HEADING.containsMatchIn(line)) headings.add(line)
        }

        return headings.joinToString("\n")
    }

    /**
     * 울타리를 만났을 때 열림 상태를 갱신합니다. 닫힌 상태면 null입니다.
     *
     * 닫는 울타리는 여는 것과 같은 문자이고 길이가 같거나 더 길어야 합니다.
     * 그래야 블록 안에 짧은 울타리를 예시로 넣은 문서에서 블록이 일찍 닫히지 않습니다.
     */
    private fun toggle(
        openFence: String?,
        fence: String,
    ): String? {
        if (openFence == null) return fence

        val closes = fence[0] == openFence[0] && fence.length >= openFence.length
        return if (closes) null else openFence
    }

    companion object {
        // 토큰이 아니라 문자 수로 센다. 캡의 목적은 정확한 계량이 아니라 폭발 방지라 토크나이저를 붙이지 않는다.
        //
        // 파일당 상한이 3만인 것은 중견 프로젝트 README가 그 언저리이기 때문이다. 여기가 낮으면 1등 문서가
        // 통째로 헤딩으로 떨어져, 개념 추출이 목차만 보고 개념을 지어내는 자리에 놓인다(레디스 README가 29,193자).
        // 예산을 상한의 두 배 넘게 두는 것은 그 1등 문서가 나머지를 밀어내지 않게 하기 위해서다.
        private const val BUDGET_CHARS = 80_000
        private const val PER_DOCUMENT_CAP_CHARS = 30_000
        private const val OUTLINE_FALLBACK_LINES = 40
        private const val BOM = "﻿"

        // `#`는 마크다운, `=`는 AsciiDoc 제목 표기다.
        private val HEADING = Regex("""^\s{0,3}(#{1,6}|={1,6})\s+\S""")

        // 여는 줄 뒤에는 ```kotlin 처럼 언어 이름이 붙으므로 줄 끝을 고정하지 않는다.
        // AsciiDoc의 ---- 는 일부러 뺐다. 마크다운에서 같은 모양이 가로줄로 쓰여 문서 절반을 코드로 오인한다.
        private val FENCE = Regex("""^ {0,3}(`{3,}|~{3,})""")
    }
}
