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

`main`에 push(=PR 머지)되면 `.github/workflows/cd.yml`이 실행되어 Docker 이미지를
`ghcr.io/nexters/git-it-server`에 push하고, 가비아 서버에 SSH로 접속해 재배포합니다.

### 필요한 GitHub Secrets

| Secret | 용도 |
|---|---|
| `GABIA_HOST` | 가비아 서버 고정 공인 IP |
| `GABIA_USERNAME` | SSH 접속 유저 |
| `GABIA_SSH_KEY` | 배포 전용 SSH 개인키 |
| `PROD_ENV_FILE` | 운영 `.env` 파일 전체 내용 |

자세한 배경과 서버 사전 준비사항은
[`docs/superpowers/specs/2026-08-03-ci-cd-design.md`](docs/superpowers/specs/2026-08-03-ci-cd-design.md)를
참고하세요.
