package com.Coming.Backend.auth.repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TokenRepository {

    private static final String KEY_PREFIX = "RT:";

    private final RedisTemplate<String, String> redisTemplate;

    public TokenRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Long userId, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + userId, refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public Optional<String> find(Long userId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return Optional.ofNullable(value);
    }

    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
