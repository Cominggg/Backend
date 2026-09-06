package com.Coming.Backend.admin.repository;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ArtistCollectLockRepository {

    private static final String KEY_PREFIX = "ARTIST_COLLECT_LOCK:";
    // WebClient responseTimeout(120s)보다 여유 있게 잡아, 정상 흐름에서는 항상 명시적 unlock으로
    // 해제되고 TTL은 서버 재시작 등 예외 상황의 안전망 역할만 한다.
    private static final long TTL_SECONDS = 150;

    private final RedisTemplate<String, String> redisTemplate;

    public ArtistCollectLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String mbid) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + mbid, "1", TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    public void unlock(String mbid) {
        redisTemplate.delete(KEY_PREFIX + mbid);
    }
}
