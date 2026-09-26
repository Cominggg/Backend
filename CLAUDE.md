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
| Batch / Mail | Spring Batch, Spring Mail + Thymeleaf |
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
├── rating/        # 공연·발매 별점
├── post/          # 커뮤니티 게시글·댓글, 멘션 태그, 통합 검색
├── report/        # 게시글·댓글 신고
├── notice/        # 공지사항
├── policy/        # 약관·개인정보처리방침, 개정 안내 메일(Spring Batch)
├── user/          # 마이페이지 (다가오는 공연, 관람 이력, 내 문의)
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

- `application-local.yaml`은 gitignore 대상이며, 프로젝트 훅(`.claude/hooks/guard_files.py`)이 시크릿 파일(`.env*`(`.env.example` 제외)·`application-local*`·`application-secret*`·`credentials*`·`*.secret(s)`)의 Read/Write/Edit를 자동 차단한다. 이 파일 수정이 필요하면 Claude가 직접 편집할 수 없으니, 추가할 내용을 알려주고 사용자가 직접 추가하도록 요청한다.
- 같은 훅이 git에 커밋된 Flyway 마이그레이션(`db/migration/V*.sql`)의 수정도 차단한다 (checksum 불일치 방지). 스키마 변경은 항상 새 버전 파일로 추가한다.

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
- auth 관련 코드(JWT, OAuth2, Redis 토큰 처리) 작성 시 `security-reviewer` 에이전트도 추가로 호출한다 (Coming 인증 정책 기준 전용 체크리스트 보유, 읽기 전용).

---

## 행동 원칙

전역 `~/.claude/CLAUDE.md`의 "행동 원칙"(코딩 전에 먼저 생각하라·단순함 우선·외과적 변경·목표 기반 실행)을 따른다.
