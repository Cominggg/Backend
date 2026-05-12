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

Repository interface는 domain 패키지에 위치한다. JpaRepository 확장 구현은 infra 패키지에 둔다.

---

## 객체지향 설계 원칙 (SOLID)

**SRP — 단일 책임**
- 하나의 클래스는 하나의 변경 이유만 갖는다.
- 메서드가 20줄을 넘으면 분리를 검토한다.

**OCP — 개방-폐쇄**
- 새 기능은 기존 코드를 수정하지 않고 추가한다.
- 분기가 늘어나는 구조보다 인터페이스 확장을 선호한다.

**LSP — 리스코프 치환**
- 상속보다 조합(Composition)을 선호한다.
- 상속 시 하위 타입은 상위 타입을 완전히 대체할 수 있어야 한다.

**ISP — 인터페이스 분리**
- 하나의 큰 인터페이스보다 역할별 작은 인터페이스를 만든다.
- Repository는 호출처가 필요한 메서드만 노출한다.

**DIP — 의존성 역전**
- Service는 Repository 구체 클래스가 아닌 인터페이스에 의존한다.
- 구체 구현에 직접 의존하는 코드를 작성하지 않는다.

**캡슐화**
- Entity의 필드는 모두 `private`이다.
- 상태 변경은 도메인 메서드를 통해서만 한다. `setter` 메서드를 만들지 않는다.
- Lombok `@Setter`, `@Data`는 Entity에 사용하지 않는다.

---

## Java Style Guide 핵심 규칙

- **들여쓰기**: 스페이스 4칸 (탭 사용 금지)
- **줄 길이**: 120자 이하
- **continuation indent**: 줄 바꿈 시 다음 줄은 원래 줄 기준 최소 +8 스페이스
- **중괄호**: Kernighan & Ritchie 스타일 — 여는 중괄호는 줄 끝에
- **import**: wildcard import 금지 (`import java.util.*` 불가), static import는 마지막 그룹
- **네이밍**:
  - 클래스·인터페이스: `UpperCamelCase`
  - 메서드·변수: `lowerCamelCase`
  - 상수: `UPPER_SNAKE_CASE`
  - 패키지: 소문자 단어 연속 (`com.coming.backend`)
- **어노테이션**: 선언부 바로 위, 각각 별도 줄

---

## 클린 코드 원칙

**네이밍**
- 이름만으로 의도를 파악할 수 있어야 한다. 주석으로 보완하지 않는다.
- 축약어를 쓰지 않는다 (`concertId` > `cId`, `findByArtistId` > `findById`).
- Boolean 변수·메서드는 `is`, `has`, `can` 접두사를 붙인다.

**함수**
- 한 메서드는 한 가지 일만 한다.
- 매개변수가 3개를 초과하면 DTO/record로 묶는다.
- 부정 조건보다 긍정 조건을 먼저 쓴다.

**상수**
- 매직 넘버와 매직 문자열을 상수 또는 enum으로 추출한다.
- `ErrorCode` enum에 없는 에러 문자열을 하드코딩하지 않는다.

**주석**
- WHY가 코드만으로 명확하지 않을 때만 작성한다.
- WHAT을 설명하는 주석은 작성하지 않는다.

---

## 테스트 작성 규칙

**구조**
- 모든 테스트는 `// given // when // then` 주석으로 구분한다.
- 하나의 테스트는 하나의 동작만 검증한다.

**네이밍**
```
should_[결과]_when_[조건]
예) should_throw_when_concert_not_found
    should_return_empty_list_when_no_following_artists
```

**단위 테스트** (`@ExtendWith(MockitoExtension.class)`)
- Service 계층: Repository를 `@Mock`으로 처리한다.
- 순수 도메인 로직: 의존성 없이 직접 테스트한다.

**통합 테스트** (`@SpringBootTest`)
- 실제 PostgreSQL을 사용한다. Mock DB(H2 등)를 사용하지 않는다.
- DB 상태를 각 테스트 전후로 정리한다 (`@Transactional` 또는 직접 삭제).

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
