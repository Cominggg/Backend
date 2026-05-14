package com.Coming.Backend.auth.repository;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BlacklistRepository {

    private static final String KEY_PREFIX = "BL:";
    private static final String BLACKLISTED = "1";

    private final RedisTemplate<String, String> redisTemplate;

    public BlacklistRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String accessToken, long remainingExpiryMillis) {
        if (remainingExpiryMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + accessToken, BLACKLISTED, remainingExpiryMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + accessToken));
    }
}
