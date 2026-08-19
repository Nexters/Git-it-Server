package com.nexters.gitit.domain.quizrepo

import java.nio.file.Path

/**
 * 수집이 끝난 레포 한 벌 — 파일이 풀린 자리와, 그 파일들이 어느 커밋인지.
 *
 * [repo]를 여기 실어 보내야 합니다. 해제 디렉터리 이름은 `{owner}-{name}-{sha}`이고 owner에 `-`가
 * 들어가면 경계를 가를 수 없어, 주소를 파싱한 수집 단계 말고는 좌표를 되짚을 곳이 없습니다.
 *
 * [root]는 절대 경로이고 파이프라인이 도는 동안만 씁니다 — 산출물에 실리면 저장된 학습 세트가
 * 특정 머신의 해제 위치에 묶입니다. 단계들은 상대 경로를 여기에 resolve 합니다.
 */
data class RepoCheckout(
    val root: Path,
    val repo: RepoCoordinates,
)

/**
 * 학습 세트가 어느 커밋을 보고 만들어졌는지.
 *
 * [sha]가 키에 들어가야 레포가 갱신됐을 때 옛 세트와 새 세트가 섞이지 않습니다.
 * 앵커는 라인 번호라, 커밋이 달라지면 같은 파일이라도 다른 곳을 가리킵니다.
 */
data class RepoCoordinates(
    val owner: String,
    val name: String,
    val sha: String,
)
