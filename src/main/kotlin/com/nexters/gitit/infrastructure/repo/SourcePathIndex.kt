package com.nexters.gitit.infrastructure.repo

/**
 * 문서에 적힌 경로 문자열을 실제 소스 파일 경로로 확정하는 조회 인덱스.
 *
 * 경로 확정을 LLM이 아니라 코드가 하기 위해 존재합니다.
 * LLM에게 관련 파일을 고르게 하면 실재하지 않는 경로를 지어내고, 그러면 사후에 걸러내야 합니다.
 * 문서에 적힌 문자열을 여기에 조회해 확정하면 없는 경로가 애초에 만들어지지 않습니다.
 *
 * 레포마다 새로 만드는 자료구조라 Spring 빈이 아닙니다.
 */
class SourcePathIndex(
    sources: List<String>,
) {
    private val paths: Set<String> = sources.toSet()
    private val byFileName: Map<String, List<String>> = sources.groupBy { it.substringAfterLast('/') }

    /**
     * 확정된 상대 경로를 반환하고, 확정하지 못하면 null입니다.
     *
     * 정확 일치 → 경로 접미사 일치 → 파일명 일치 순으로 찾되 **후보가 둘 이상이면 null**입니다.
     * 애매한 매칭을 통과시키는 것보다 개념 하나를 잃는 편이 쌉니다.
     * `index.js`가 40개인 레포에서 하나를 찍으면 뒷단계가 엉뚱한 파일을 읽고 콜을 버립니다.
     */
    fun resolve(candidate: String): String? {
        val normalized =
            candidate
                .trim()
                .trim('`')
                .removePrefix("./")
                .removePrefix("/")
        if (normalized.isEmpty()) return null

        if (normalized in paths) return normalized

        // 문서는 풀패스를 잘 쓰지 않는다. `repo/App.kt`가 `src/main/repo/App.kt`를 가리키는 경우를 받아준다.
        if ('/' in normalized) return paths.singleOrNull { it.endsWith("/$normalized") }

        return byFileName[normalized]?.singleOrNull()
    }
}
