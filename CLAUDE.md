# Coming Backend

아티스트 팔로우, 공연 일정, 음악 발매 정보를 제공하는 Coming 서비스의 Spring Boot 백엔드.

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| DB | PostgreSQL + Flyway |
| Cache/Session | Redis |
| Auth | OAuth2 (Google/Kakao) + JWT |
| ORM | Spring Data JPA |
| Util | Lombok |
| Build | Gradle |

## 패키지 구조

```
com.Coming.Backend
├── auth/          # 인증·인가 (JWT, OAuth2, 토큰 관리)
├── artist/        # 아티스트, 팔로우
├── concert/       # 공연, 예매 링크, 셋리스트
├── calendar/      # 사용자 공연 캘린더
├── release/       # 음악 발매 (앨범, 트랙)
├── user/          # 사용자 정보
├── inquiry/       # 문의
├── admin/         # 관리자
└── common/
    ├── config/    # Security, Redis, CORS 설정
    ├── entity/    # BaseEntity 등 공통 엔티티
    ├── exception/ # ErrorCode, GlobalExceptionHandler
    ├── response/  # 공통 응답 DTO
    ├── filter/    # JWT 인증, Rate Limit, MDC 로깅 필터
    ├── aspect/    # 로깅 AOP
    ├── discord/   # 4xx/5xx 알림
    └── util/      # 공통 유틸
```

## 명령어

```bash
./gradlew bootRun   # 로컬 실행
./gradlew test      # 전체 테스트
./gradlew test --tests "com.Coming.Backend.artist.service.ArtistServiceTest"  # 단일 클래스 테스트
./gradlew build     # 빌드
```

> 로컬 실행 전 PostgreSQL(Homebrew 서비스)과 Redis(Docker)가 구동 중이어야 한다.
> - PostgreSQL: `brew services start postgresql@18`
> - Redis: `docker start redis` (Docker 데몬이 꺼져 있으면 `open -a Docker`로 먼저 기동)
> - Redis 미기동 시 증상: 연결 실패가 Bean 생성 예외로 나타나 원인이 바로 드러나지 않으니 주의

## 로컬 모니터링 / 부하 테스트

`docker-compose.monitoring.yml`(Prometheus/Grafana), `load-test/k6/`(부하 테스트 스크립트) 참고.

## 알려진 제약

- `application-local.yaml`은 gitignore 대상이며, 프로젝트 훅이 파일명에 `application-local`/`application-prod`/`.env`/`credentials`가 포함된 파일의 Write/Edit를 자동 차단한다 (`.claude/settings.json`). 이 파일 수정이 필요하면 Claude가 직접 편집할 수 없으니, 추가할 내용을 알려주고 사용자가 직접 추가하도록 요청한다.

## 명세 위치 (Cominggg/Specification)

명세는 외부 레포(`Cominggg/Specification`)에 있다. `/read-spec` 스킬로 접근한다.

## 핵심 도메인 규칙

**인증 정책**
- Access Token: 30분, `Authorization: Bearer {token}` 헤더
- Refresh Token: 7일, HttpOnly Cookie
- 로그아웃: Redis 블랙리스트 등록

**에러 응답 형식**
```json
{ "code": "CONCERT_NOT_FOUND", "message": "존재하지 않는 공연입니다." }
```

**페이지네이션 응답 형식**
```json
{ "content": [], "page": 0, "size": 20, "totalElements": 100, "totalPages": 5 }
```

---

## 개발 워크플로우

### 전체 흐름

```
구현 (클래스 단위) → 테스트 작성 → /be-review → /simplify → /commit → /pr
```

### 테스트 작성 시점

- **각 클래스의 작업 범위 내 구현이 완료된 직후** 테스트를 작성한다. 신규 클래스는 전체 메서드, 기존 클래스는 추가·변경된 메서드에 대한 테스트만 작성한다.
- `/be-review` 실행 전, `./gradlew test`가 통과된 상태여야 한다.
- 테스트 작성 시 `write-tests` 에이전트를 사용한다. (예: "ArtistService 테스트 작성해줘")

### 병렬 테스트 작성

- **3개 이상 도메인**: `write-tests` 에이전트를 단일 응답에서 병렬 호출한다
- **1~2개 도메인**: 순차 작성한다 (cold start 중복 비용이 시간 이득을 초과)
- **에이전트 호출 전**: `build.gradle`과 기존 테스트 예제 파일 1개를 먼저 읽고 프롬프트에 포함해 에이전트의 중복 파일 탐색을 방지한다
- **레이어 간 순서 유지**: 동일 도메인 내 Service + Controller 테스트는 순서대로 작성한다 (Controller는 Service 계약을 전제)

### 커밋 전 체크리스트

- `/be-review` 통과(🔴 critical 0건) 전에 `/commit`을 실행하지 않는다.
- auth 관련 코드(JWT, OAuth2, Redis 토큰 처리) 작성 시 `/security-review`도 추가 실행한다.

---

## 행동 원칙

> Adapted from [andrej-karpathy-skills/CLAUDE.md](https://github.com/forrestchang/andrej-karpathy-skills/blob/main/CLAUDE.md)
> These guidelines bias toward caution over speed. For trivial tasks, use judgment.

### 1. 코딩 전에 먼저 생각하라

- 가정을 명시적으로 밝혀라. 불확실하면 물어봐라.
- 여러 해석이 가능하면 모두 제시하고, 조용히 하나를 고르지 마라.
- 더 단순한 방법이 있으면 말해라. 필요하면 반박해라.
- 무언가 불분명하면 멈춰라. 무엇이 헷갈리는지 이름 붙이고 물어봐라.

### 2. 단순함 우선

- 요청된 것 이상의 기능을 만들지 마라.
- 단일 사용 코드에 추상화를 만들지 마라.
- 요청되지 않은 유연성이나 설정 가능성을 넣지 마라.
- 불가능한 시나리오에 대한 에러 핸들링을 만들지 마라.
- 200줄로 쓴 코드가 50줄로 가능하면 다시 써라.

### 3. 외과적 변경

- 요청된 코드만 수정하라. 인접한 코드, 주석, 포맷을 "개선"하지 마라.
- 망가지지 않은 것을 리팩터링하지 마라.
- 기존 스타일이 마음에 들지 않아도 맞춰라.
- 관련 없는 데드코드를 발견하면 언급만 하고, 삭제하지 마라.

### 4. 목표 기반 실행

작업을 검증 가능한 목표로 변환하라:
- "검증 추가" → "잘못된 입력 테스트 작성 후 통과"
- "버그 수정" → "재현 테스트 작성 후 통과"

다단계 작업은 계획을 먼저 제시하라:
```
1. [단계] → 검증: [체크]
2. [단계] → 검증: [체크]
```
