# git-it

오픈소스 코드베이스를 "읽는" 대신 "질문에 답하며" 익히도록 돕는 Kotlin + Spring Boot 서버.

## 서비스 목적

개발자가 오픈소스의 **코드 구조와 핵심 개발 맥락**을, AI가 생성한 질문을 기반으로 학습할 수 있도록 돕는 것을 목표로 합니다. 저장소를 훑어보는 것만으로는 잘 남지 않는 설계 의도와 흐름을, 질문에 답해보는 과정을 통해 체득하게 하는 것이 이
서버의 역할입니다.

## 기술 스택

| 구분         | 사용 기술                       |
|--------------|---------------------------------|
| 언어         | Kotlin 2.3.21                   |
| 런타임       | JDK 25 (Gradle toolchain)       |
| 프레임워크   | Spring Boot 4.1.0 (Spring MVC)  |
| 데이터베이스 | MongoDB 8 (Spring Data MongoDB) |

## 패키지 구조

베이스 패키지는 `com.nexters.gitit` 입니다.

```
com.nexters.gitit
├── domain              ← 순수 비즈니스 로직 (외부 의존 없음)
│   ├── model
│   └── repository      (인터페이스만)
│
├── application         ← domain에만 의존, 유스케이스 처리
│   └── service
│
├── infrastructure      ← 기술적 관심사, domain 인터페이스의 구현체 (DIP)
│   └── persistence
│
└── ui                  ← application만 호출
    └── controller
```

**의존 방향**

```
ui → application → domain ← infrastructure
```

`infrastructure`는 기술적 관심사를 담당하며 `domain`이 선언한 인터페이스의 구현체를 두므로, 의존성 역전 (DIP)에 따라 화살표가 `domain` 쪽을 향합니다.

## 시작하기

### 사전 요구사항

- JDK 25
- Docker (로컬 MongoDB 및 테스트용 Testcontainers 실행)

### 1. 환경 변수

```bash
cp .env.example .env
```

`.env.example`에서 값이 비어 있는 항목은 채워야 합니다. 채우지 않으면 기동에 실패합니다.

> Spring Boot는 `.env` 파일을 자동으로 읽지 않습니다. 셸에 `export` 하거나 IDE 실행 구성의 환경 변수에 넣어야 합니다.

### 2. 인프라 실행

```bash
docker compose -f docker-compose.local.yml up -d
```

`mongo:8` 컨테이너 (`git-it-mongodb`)가 `27017` 포트로 뜨고, 데이터는 `mongodb-data` 볼륨에 유지됩니다. healthcheck가 포함되어 있으므로 준비 상태는 아래로 확인합니다.

```bash
docker compose -f docker-compose.local.yml ps
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

- 서버: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI 문서: http://localhost:8080/v3/api-docs

### 4. 종료

```bash
docker compose -f docker-compose.local.yml down     # 컨테이너만 정리
docker compose -f docker-compose.local.yml down -v  # 저장된 데이터까지 삭제
```

## 테스트

```bash
./gradlew test
```

## 코드 스타일 / 정적 분석

```bash
./gradlew ktlintCheck detekt  # 검사
./gradlew ktlintFormat        # 자동 포맷
```

- 포맷 규칙은 [`.editorconfig`](.editorconfig)를 따릅니다.
- detekt 규칙은 [`config/detekt/detekt.yml`](config/detekt/detekt.yml)에서 관리합니다.
- PR을 올리기 전 확인 항목은 [PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md)의 체크리스트를 참고하세요.

## 배포 (CD)

`main`에 push(=PR 머지)되면 `.github/workflows/cd.yml`이 실행되어 이미지를
`ghcr.io/nexters/git-it-server`에 push하고, 서버에 SSH로 접속해 재배포합니다.
태그는 커밋 SHA와 `latest` 두 개가 붙고, 서버는 `latest`를 pull합니다.

### 이미지 빌드

Dockerfile 없이 [Jib](https://github.com/GoogleContainerTools/jib) Gradle 플러그인이 이미지를 만듭니다.
설정은 `build.gradle.kts`의 `jib { }` 블록에 있습니다.

```bash
./gradlew jibDockerBuild --image=git-it-server:local  # 로컬 Docker 데몬에만 빌드
```

베이스 이미지는 `eclipse-temurin:25-jre`이고 non-root(uid 1000)로 실행됩니다.

### HTTPS (Nginx + Certbot)

운영 서버는 https://git-it.kr 로 서비스합니다. `docker-compose.prod.yml`이 앱·MongoDB와 함께
Nginx와 Certbot을 띄웁니다.

- Nginx가 80·443을 받아 `app:8080`으로 넘깁니다. 80으로 들어온 요청은 ACME 검증 경로
  (`/.well-known/acme-challenge/`)만 통과시키고 나머지는 HTTPS로 리다이렉트합니다.
- 설정은 `nginx/nginx.conf`이며 배포마다 서버로 전송됩니다.
- 인증서는 `~/git-it/certbot/conf`에 보관됩니다. 최초 발급은 배포 스크립트가 standalone
  방식으로 한 번 수행하고, 이후 갱신은 Certbot 컨테이너가 12시간마다 webroot 방식으로
  처리합니다. Nginx는 6시간마다 reload해 갱신된 인증서를 집어 옵니다.

서버에서 80·443 인바운드가 열려 있어야 합니다. 80이 막히면 ACME 검증이 실패해 발급 자체가
되지 않습니다.

### 수동 실행

`Actions → Backend CD → Run workflow`로 브랜치를 골라 실행할 수 있습니다.

> ⚠️ 수동 실행은 `main` push와 **완전히 동일하게** 동작합니다. 고른 브랜치의 코드가
> 그대로 운영에 배포되고 `latest` 태그도 그 이미지로 옮겨갑니다. 되돌리려면 `main`을
> 다시 배포해야 합니다.

### 필요한 GitHub Secrets

| Secret | 용도 |
|---|---|
| `API_SERVER_HOST` | 서버 고정 공인 IP |
| `API_SERVER_USERNAME` | SSH 접속 유저 |
| `API_SERVER_KEY` | 배포 전용 SSH 개인키 |
| `API_SERVER_PORT` | SSH 포트 |

배포 스크립트가 아래 값들로 서버의 `.env`를 조립합니다. 이름과 의미는 `.env.example`과 같습니다.

| Secret | 비고 |
|---|---|
| `MONGODB_HOST` | **`mongodb`** (docker-compose 서비스명) |
| `MONGODB_PORT` | `27017` |
| `MONGODB_DATABASE` | |
| `MONGODB_USERNAME` | |
| `MONGODB_PASSWORD` | |
| `JWT_SECRET` | |
| `OAUTH_GOOGLE_CLIENT_ID` | |
| `OAUTH_APPLE_CLIENT_ID` | |
| `WORK_DIR` | 앱 컨테이너 안의 경로. uid 1000으로 도니 `/tmp/gitit-work`처럼 쓸 수 있는 곳이어야 합니다 |
| `GCP_CREDENTIALS_BASE64` | 서비스 계정 JSON을 base64로 인코딩한 값 |

> 시크릿 이름에 `GITHUB_` 접두사를 쓸 수 없습니다. Actions가 그 접두사로 시작하는 시크릿을 거부하는데,
> 워크플로에서는 오류가 아니라 빈 문자열로 치환돼 조용히 잘못된 값이 배포됩니다.

> `MONGODB_HOST`를 `localhost`로 두면 안 됩니다. 운영에서는 `app`과 `mongodb`가 같은 Docker
> 네트워크의 별개 컨테이너이므로 `localhost`는 app 컨테이너 자신을 가리켜 연결에 실패합니다.

GHCR 인증은 워크플로가 `GITHUB_TOKEN`으로 `docker login`한 결과(`~/.docker/config.json`)를
Jib이 읽어 가므로 별도 시크릿이 필요 없습니다.

서버 사전 준비사항은
[`docs/superpowers/specs/2026-08-03-ci-cd-design.md`](docs/superpowers/specs/2026-08-03-ci-cd-design.md)를
참고하세요. 단, 이 문서는 Dockerfile로 빌드하던 시점에 작성되어 이미지 빌드 부분은 현재와 다릅니다.
