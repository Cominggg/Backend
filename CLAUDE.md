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
| Build | Gradle |

## 패키지 구조

```
com.Coming.Backend
├── auth/          # 인증·인가 (JWT, OAuth2, 토큰 관리)
├── artist/        # 아티스트, 팔로우
├── concert/       # 공연, 예매 링크, 셋리스트
├── calendar/      # 사용자 공연 캘린더
├── release/       # 음악 발매 (앨범, 트랙)
├── inquiry/       # 문의
├── admin/         # 관리자
└── common/
    ├── config/    # Security, Redis, CORS 설정
    ├── exception/ # ErrorCode, GlobalExceptionHandler
    └── response/  # 공통 응답 DTO
```

## 명령어

```bash
./gradlew bootRun   # 로컬 실행
./gradlew test      # 전체 테스트
./gradlew build     # 빌드
```

## 명세 위치 (Cominggg/Specification)

- ERD: `spec/erd.md`
- 인증 정책: `spec/auth-policy.md`
- API 전체: `spec/api/_index.md`

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

코드 작성 완료 후 반드시 아래 순서를 지킨다:

```
/be-review → /simplify → /commit → /pr
```

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
