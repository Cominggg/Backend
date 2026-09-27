package com.Coming.Backend.auth.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

/**
 * Refresh Token을 기기(세션) 단위로 저장한다.
 * <ul>
 *   <li>{@code RT:{userId}:{sessionId}} — 세션별 Refresh Token</li>
 *   <li>{@code RT_SESSIONS:{userId}} — 사용자의 세션 목록 ZSET (score = 세션 생성 시각)</li>
 * </ul>
 */
@Repository
public class TokenRepository {

    private static final String KEY_PREFIX = "RT:";
    private static final String SESSIONS_KEY_PREFIX = "RT_SESSIONS:";
    private static final int MAX_SESSIONS = 5;

    // 저장된 Refresh Token이 일치할 때만 교체한다. 같은 토큰으로 동시에 들어온 refresh 중 1건만 성공시키고,
    // 세션이 refresh로만 연장되는 경우에도 세션 목록이 먼저 만료되지 않도록 목록 TTL을 함께 연장한다.
    private static final RedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) ~= ARGV[1] then return 0 end "
                    + "redis.call('set', KEYS[1], ARGV[2], 'PX', ARGV[3]) "
                    + "redis.call('pexpire', KEYS[2], ARGV[3]) "
                    + "return 1",
            Long.class);

    private final RedisTemplate<String, String> redisTemplate;

    public TokenRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 새 세션을 저장한다. 세션이 최대 개수를 넘으면 가장 오래 전에 생성된 세션부터 제거한다.
     */
    public void saveSession(Long userId, String sessionId, String refreshToken, long ttlMillis) {
        String sessionsKey = sessionsKey(userId);
        redisTemplate.opsForValue()
                .set(sessionKey(userId, sessionId), refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
        redisTemplate.opsForZSet().add(sessionsKey, sessionId, System.currentTimeMillis());
        redisTemplate.expire(sessionsKey, ttlMillis, TimeUnit.MILLISECONDS);

        pruneSessions(userId);
    }

    /**
     * 저장된 Refresh Token이 currentToken과 일치할 때만 newToken으로 교체한다.
     *
     * @return 교체에 성공하면 true, 세션이 없거나 값이 다르면 false
     */
    public boolean rotate(Long userId, String sessionId, String currentToken, String newToken,
            long ttlMillis) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(sessionKey(userId, sessionId), sessionsKey(userId)),
                currentToken, newToken, String.valueOf(ttlMillis));
        return Long.valueOf(1L).equals(result);
    }

    public void deleteSession(Long userId, String sessionId) {
        redisTemplate.delete(sessionKey(userId, sessionId));
        redisTemplate.opsForZSet().remove(sessionsKey(userId), sessionId);
    }

    public void deleteAllSessions(Long userId) {
        String sessionsKey = sessionsKey(userId);
        Set<String> sessionIds = redisTemplate.opsForZSet().range(sessionsKey, 0, -1);
        List<String> keys = new ArrayList<>();
        if (sessionIds != null) {
            sessionIds.forEach(sessionId -> keys.add(sessionKey(userId, sessionId)));
        }
        keys.add(sessionsKey);
        redisTemplate.delete(keys);
    }

    // 만료된 세션을 목록에서 정리한 뒤, 남은 세션이 최대 개수를 넘으면 오래된 순으로 제거한다.
    // score(생성 시각)는 refresh로 연장된 세션을 구분하지 못하므로, 만료는 Refresh Token 키 존재 여부로 판단한다.
    private void pruneSessions(Long userId) {
        String sessionsKey = sessionsKey(userId);
        Set<String> sessionIds = redisTemplate.opsForZSet().range(sessionsKey, 0, -1);
        if (sessionIds == null) {
            return;
        }
        List<String> activeSessionIds = new ArrayList<>();
        for (String sessionId : sessionIds) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(userId, sessionId)))) {
                activeSessionIds.add(sessionId);
            } else {
                redisTemplate.opsForZSet().remove(sessionsKey, sessionId);
            }
        }
        for (int i = 0; i < activeSessionIds.size() - MAX_SESSIONS; i++) {
            deleteSession(userId, activeSessionIds.get(i));
        }
    }

    private String sessionKey(Long userId, String sessionId) {
        return KEY_PREFIX + userId + ":" + sessionId;
    }

    private String sessionsKey(Long userId) {
        return SESSIONS_KEY_PREFIX + userId;
    }
}
