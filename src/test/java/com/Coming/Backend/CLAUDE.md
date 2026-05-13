# 테스트 작성 규칙

테스트 코드 작성 시 `write-tests` 에이전트를 사용한다.
전체 컨벤션·작성 절차·코드 예시는 `.claude/agents/write-tests.md`가 단일 출처다.

---

## 핵심 요약

| 대상 | 어노테이션 |
|------|-----------|
| Service, Domain 로직 | `@ExtendWith(MockitoExtension.class)` |
| Controller | `@WebMvcTest(XxxController.class)` |
| Repository | `@DataJpaTest` |
| 통합 테스트 | `@SpringBootTest` (꼭 필요한 경우에만) |

- **네이밍**: `should_{결과}_{when|given}_{조건}`
- **구조**: 모든 테스트에 `// given / // when / // then` 주석
- **Assertion**: AssertJ 사용. JUnit 5 기본 `assertEquals` 금지.
- **통합 테스트**: 실제 PostgreSQL 사용. H2 대체 금지. 각 테스트 전후 DB 상태 정리.
