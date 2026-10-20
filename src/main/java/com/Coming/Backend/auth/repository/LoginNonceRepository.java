package com.Coming.Backend.auth.repository;

import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 앱 소셜 로그인용 1회용 nonce 저장소. 서버가 발급한 nonce만, 한 번만 쓸 수 있게 해 ID Token 재전송을 막는다.
 */
@Repository
public class LoginNonceRepository {

    private static final String KEY_PREFIX = "NONCE:";
    private static final String ISSUED = "1";

    private final RedisTemplate<String, String> redisTemplate;

    public LoginNonceRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String nonce, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + nonce, ISSUED, ttl);
    }

    /**
     * nonce가 발급된 상태면 지우고 true를 반환한다. 원자적으로 지우므로 같은 nonce는 한 번만 true다.
     */
    public boolean consume(String nonce) {
        return Boolean.TRUE.equals(redisTemplate.delete(KEY_PREFIX + nonce));
    }
}
