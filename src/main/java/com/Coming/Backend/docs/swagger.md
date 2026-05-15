## Swagger 어노테이션 규칙

| 어노테이션 | 적용 대상 | 작성 기준 |
|-----------|----------|---------|
| `@Tag(name = "...")` | Controller 클래스 | 도메인명 단수형 (예: `"Artist"`, `"Auth"`) |
| `@Operation(summary = "...")` | Controller 메서드 전체 | 동사 + 목적어 (예: `"아티스트 목록 조회"`) |
| `@ApiResponse` | Controller 메서드 | 200 외 응답 코드에만 명시 |

**적용 범위**: Controller 레이어 전체. Service Javadoc을 대체하지 않는다.
