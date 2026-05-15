## Javadoc 작성 기준

- **적용 범위**: Service 레이어 public 메서드 전체 (신규·기존 모두)
- **Controller**: Javadoc 대신 `@Operation(summary = "...")` 사용
- **형식**: 첫 줄 한 문장 요약. 파라미터가 자명하지 않을 때만 `@param` 추가. `@return`은 반환값이 명확하지 않을 때만 작성

```java
/**
 * Refresh Token을 검증하고 새 Access Token을 발급한다.
 *
 * @param refreshToken HttpOnly Cookie에서 추출한 Refresh Token
 */
public TokenResponse refreshToken(String refreshToken) { ... }
```
