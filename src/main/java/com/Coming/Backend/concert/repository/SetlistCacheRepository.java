package com.Coming.Backend.concert.repository;

import com.Coming.Backend.concert.dto.SetlistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * setlist.fm에서 받은 셋리스트를 setlist.fm ID 기준으로 잠시 보관한다.
 *
 * <p>setlist.fm 약관(§3)은 데이터 사본 보관을 금지하고 "short periods" 캐시만 허용한다.
 * 그래서 TTL이 지나면 Redis가 지우고, 만료된 값을 다른 곳에 남겨 두지 않는다.</p>
 */
@Repository
@RequiredArgsConstructor
public class SetlistCacheRepository {

    private static final String KEY_PREFIX = "SETLIST_FM:";
    static final Duration TTL = Duration.ofHours(12);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<SetlistResponse> find(String setlistFmId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + setlistFmId);
        return Optional.ofNullable(json).map(value -> objectMapper.readValue(value, SetlistResponse.class));
    }

    public void save(String setlistFmId, SetlistResponse setlist) {
        redisTemplate.opsForValue().set(KEY_PREFIX + setlistFmId, objectMapper.writeValueAsString(setlist), TTL);
    }
}
