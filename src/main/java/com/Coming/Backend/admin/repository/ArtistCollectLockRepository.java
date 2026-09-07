package com.Coming.Backend.admin.repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class ArtistCollectLockRepository {

    private static final String KEY_PREFIX = "ARTIST_COLLECT_LOCK:";
    // WebClient responseTimeout(120s)보다 여유 있게 잡아, 정상 흐름에서는 항상 명시적 unlock으로
    // 해제되고 TTL은 서버 재시작 등 예외 상황의 안전망 역할만 한다.
    private static final long TTL_SECONDS = 150;

    // 저장된 토큰이 일치할 때만 삭제한다. TTL 만료 후 다른 요청이 같은 키를 선점했다면,
    // 이전 보유자의 unlock이 그 새 락을 실수로 지우지 않도록 막는다.
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    private final RedisTemplate<String, String> redisTemplate;

    public ArtistCollectLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * mbid에 대한 락을 선점한다.
     *
     * @return 선점에 성공하면 이 호출을 식별하는 토큰, 이미 다른 요청이 보유 중이면 null
     */
    public String tryLock(String mbid) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + mbid, token, TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    /**
     * 전달된 토큰이 현재 저장된 값과 일치할 때만 락을 해제한다.
     */
    public void unlock(String mbid, String token) {
        redisTemplate.execute(UNLOCK_SCRIPT, List.of(KEY_PREFIX + mbid), token);
    }
}
