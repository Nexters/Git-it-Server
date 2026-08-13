# CI/CD 파이프라인 설계

- Status: Approved
- Date: 2026-08-03
- Branch: feat/TASK-85-ci-cd

## 배경

`.github/workflows/ci.yml`에 PR 기준 lint/test/build(CI)는 이미 구성되어 있다. 배포(CD)는
아직 없다. 배포 대상은 가비아(Gabia) 클라우드 서버이며, 이 스펙 작성 시점에는 서버가
아직 발급되지 않았다. 서버 발급 전에도 준비 가능한 코드/워크플로를 먼저 구성하고,
서버가 준비되면 GitHub Secrets만 채워 넣어 바로 동작하도록 한다.

## 목표

- `main`에 push(=PR 머지)되면 Docker 이미지를 빌드해 GHCR에 push
- 이후 가비아 서버에 SSH로 접속해 새 이미지를 pull & 재기동
- 서버 자원은 최소한으로: 인스턴스 1대에 app + MongoDB를 함께 컨테이너로 구동, 리버스
  프록시/HTTPS/스테이징 환경 등은 이번 범위에서 제외 (도메인 연결은 인프라 단에서 처리,
  파이프라인 코드에는 영향 없음)

## 비목표 (범위 제외)

- 무중단 배포(blue-green, rolling 등) — 이번엔 단순 재기동으로 충분
- 스테이징 환경 분리
- 리버스 프록시/HTTPS 구성 (Nginx, Let's Encrypt 등)
- GitHub Secrets 값 입력, 가비아 인프라 자원 생성 — 사용자가 인프라 콘솔에서 직접 수행

## 아키텍처

```
PR → main                          main에 merge(push)
  ├─ ci.yml (기존, 유지)              ├─ cd.yml (신규)
  │   ktlintCheck, detekt,           │   1) Docker 이미지 빌드
  │   test, bootJar                  │   2) GHCR push (tag: sha, latest)
  │                                  │   3) SSH로 가비아 서버 접속
  │                                  │      → docker compose pull && up -d
```

## 변경/추가 파일

### 1. `Dockerfile` (신규, 프로젝트 루트)

멀티스테이지 빌드:
- 빌드 스테이지: Gradle + JDK 25로 `./gradlew bootJar` 실행
- 실행 스테이지: `eclipse-temurin:25-jre` 기반, 빌드 스테이지에서 만든 jar만 복사해 실행

### 2. `.github/workflows/cd.yml` (신규)

- 트리거: `push` to `main`
- Job `build-and-push`:
  - checkout
  - Docker Buildx 설정
  - GHCR 로그인 (`GITHUB_TOKEN` 사용, 추가 시크릿 불필요)
  - 이미지 빌드 & push: `ghcr.io/nexters/git-it-server:{sha}`, `:latest`
- Job `deploy` (needs: `build-and-push`):
  - `appleboy/ssh-action`으로 가비아 서버 접속
  - 서버에서 `docker compose -f docker-compose.prod.yml pull && up -d --remove-orphans`
  - `docker image prune -f`로 오래된 이미지 정리

### 3. `docker-compose.prod.yml` (신규)

- `app` 서비스: `ghcr.io/nexters/git-it-server:latest`, `env_file: .env`, 포트 `8080:8080`
- `mongodb` 서비스: `docker-compose.local.yml`과 동일한 이미지/설정 패턴이되 **호스트에 포트
  노출하지 않음** (앱 컨테이너와 같은 Docker 네트워크로만 통신, 외부 27017 접근 차단)
- `.env`는 저장소에 커밋하지 않고, 배포 스크립트가 매 배포마다 GitHub Secret
  (`PROD_ENV_FILE`)의 내용을 서버의 `.env` 파일로 덮어씀

### 4. `README.md`

배포 섹션 짧게 추가: CD 트리거 조건, 필요한 GitHub Secrets 목록, 서버 사전 준비사항 링크.

## 필요한 GitHub Secrets (서버 준비 후 사용자가 직접 등록)

| Secret | 용도 |
|---|---|
| `GABIA_HOST` | 가비아 서버 고정 공인 IP |
| `GABIA_USERNAME` | SSH 접속 유저 |
| `GABIA_SSH_KEY` | 배포 전용 SSH 개인키 |
| `PROD_ENV_FILE` | `.env` 파일 전체 내용 (JWT_SECRET, OAUTH 키, MongoDB 인증정보 등) |

시크릿 값 자체는 이 저장소/대화에 노출하지 않고 GitHub 저장소 Settings에서 직접 입력한다.

## 사용자가 별도로 준비할 인프라 (파이프라인 코드와 무관, 참고용)

- 가비아 g클라우드 서버 1대 (Ubuntu 22.04, 2vCPU/4GB 권장)
- 고정(유동 아님) 공인 IP — 도메인 연결 및 SSH 시크릿 안정성을 위해 필수
- 방화벽: `22`, `8080`만 오픈, `27017`은 막음
- 서버 내 Docker Engine + Compose plugin 설치, 배포 유저를 docker 그룹에 추가
- 서버에서 `docker login ghcr.io` 1회 실행 (GHCR pull 인증)
- 도메인 A레코드 → 고정 IP 연결 (선택, 파이프라인 동작에는 필수 아님)

## 테스트 계획

- `Dockerfile`: 로컬에서 `docker build`, `docker run`으로 이미지가 정상 기동하는지 확인
  (MongoDB는 `docker-compose.local.yml`로 띄운 뒤 연결 테스트)
- `cd.yml`: 서버 시크릿이 없는 상태에서는 `build-and-push` job까지만 성공하는지 확인,
  `deploy` job은 시크릿 등록 전까지는 실패가 예상되는 상태로 둔다 (서버 준비 후 검증)
- `docker-compose.prod.yml`: 로컬에서 `docker compose -f docker-compose.prod.yml config`로
  문법 검증
