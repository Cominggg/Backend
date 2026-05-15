# 코딩 원칙

Java 파일 작업 시 항상 적용한다.

---

## DDD 레이어 규칙

레이어 의존 방향은 단방향이다: `Controller → Service → Domain ← Repository`

| 레이어 | 책임 | 금지 사항 |
|--------|------|----------|
| **Controller** | HTTP 요청/응답 변환, 파라미터 검증 | 비즈니스 로직, Repository 직접 호출 |
| **Service** | 유스케이스 오케스트레이션, 트랜잭션 경계 | 도메인 로직 직접 구현 |
| **Domain (Entity)** | 비즈니스 상태·규칙 보유 | 외부 의존성 (Spring Bean 등) 주입 |
| **Repository** | 영속성 추상화 | — |

**레이어 간 데이터 전달은 DTO로 한다. Entity를 Controller까지 올리지 않는다.**

Repository interface와 JpaRepository 확장 구현 모두 각 도메인의 repository 패키지에 위치한다.

---

## 객체지향 설계 원칙

- **SRP**: 메서드가 20줄을 넘으면 분리를 검토한다.
- **OCP**: 분기가 늘어나는 구조보다 인터페이스 확장을 선호한다.
- **LSP**: 상속보다 조합(Composition)을 선호한다.
- **ISP**: Repository는 호출처가 필요한 메서드만 노출한다.
- **DIP**: Service는 Repository 구체 클래스가 아닌 인터페이스에 의존한다.

**캡슐화**
- Entity의 필드는 모두 `private`. 상태 변경은 도메인 메서드를 통해서만 한다. `setter` 메서드 금지.
- Lombok `@Setter`, `@Data`는 Entity에 사용하지 않는다.
- Entity에 허용: `@Getter`, `@Builder`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`

**Lombok 사용 규칙**

| 어노테이션 | 적용 대상 | 사용 조건 |
|-----------|----------|---------|
| `@Getter` | Entity, Enum, 추상 클래스 등 수동 getter가 필요한 모든 클래스 | 단순 필드 반환 getter를 직접 작성하지 않는다 |
| `@RequiredArgsConstructor` | Service, Component, Filter 등 Spring Bean 클래스 | 생성자 내부 로직(필드 변환, super 호출 등)이 없을 때만 적용 |
| `@Slf4j` | 로그가 필요한 모든 클래스 | 예외 처리, 주요 이벤트(로그인, 신규 사용자 생성 등) 지점에 작성 |

로그 레벨 기준:
- `DEBUG`: 필터 내 토큰 파싱 실패 등 정상 흐름에서 발생 가능한 낮은 수준 이벤트
- `INFO`: 신규 사용자 생성, 로그인 성공 등 비즈니스 이벤트
- `WARN`: OAuth2 실패, 예상 가능한 비즈니스 예외(`BusinessException`)
- `ERROR`: 예상치 못한 서버 오류 (`Exception` catch-all)

---

## 코드 스타일

- **import**: wildcard import 금지 (`import java.util.*` 불가), static import는 마지막 그룹
- **네이밍**: 클래스·인터페이스 `UpperCamelCase` / 메서드·변수 `lowerCamelCase` / 상수 `UPPER_SNAKE_CASE`
- **어노테이션**: 선언부 바로 위, 각각 별도 줄

---

## 클린 코드 원칙

- 축약어 금지 (`concertId` > `cId`, `findByArtistId` > `findById`)
- Boolean 변수·메서드는 `is`, `has`, `can` 접두사
- 매개변수 3개 초과 시 DTO/record로 묶는다
- `ErrorCode` enum에 없는 에러 문자열을 하드코딩하지 않는다

---

## Coming 도메인 컨벤션

**ErrorCode 네이밍**
- 도메인 prefix 필수: `CONCERT_NOT_FOUND`, `ARTIST_ALREADY_FOLLOWED`
- HTTP 상태와 함께 정의: `UNAUTHORIZED(401)`, `FORBIDDEN(403)`

**Flyway 마이그레이션**
- 파일명: `V{버전}__{설명}.sql` — 예) `V1__init_schema.sql`
- 한 번 적용된 파일은 수정하지 않는다. 변경은 새 버전 파일로 추가한다.

**환경변수**
- `application.yaml`에 민감 정보를 직접 쓰지 않는다.
- `${ENV_VAR:default}` 형식을 사용한다.

**필드 매핑**
- DB snake_case 컬럼은 응답 JSON에서 camelCase로 변환한다.
- DB 컬럼명과 다른 응답 필드명은 명세의 매핑 테이블을 따른다 (`venue_name` → `venue`).

---

@docs/swagger.md

@docs/javadoc.md
