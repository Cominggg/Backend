package com.Coming.Backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.Coming.Backend.auth.repository.TokenRepository.RotationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class TokenRepositoryTest {

    private static final long TTL = 604_800_000L;

    @InjectMocks
    private TokenRepository tokenRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Test
    void should_storeTokenAndRegisterSession_when_saveSession() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.range("RT_SESSIONS:1", 0, -1)).willReturn(Set.of("s1"));
        given(redisTemplate.hasKey("RT:1:s1")).willReturn(true);

        // when
        tokenRepository.saveSession(1L, "s1", "token", TTL);

        // then
        verify(valueOperations).set("RT:1:s1", "token", TTL, TimeUnit.MILLISECONDS);
        verify(zSetOperations).add(eq("RT_SESSIONS:1"), eq("s1"), anyDouble());
        verify(redisTemplate).expire("RT_SESSIONS:1", TTL, TimeUnit.MILLISECONDS);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void should_removeSessionFromList_when_itsTokenHasExpired() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.range("RT_SESSIONS:1", 0, -1)).willReturn(orderedSet("expired", "s1"));
        given(redisTemplate.hasKey("RT:1:expired")).willReturn(false);
        given(redisTemplate.hasKey("RT:1:s1")).willReturn(true);

        // when
        tokenRepository.saveSession(1L, "s1", "token", TTL);

        // then
        verify(zSetOperations).remove("RT_SESSIONS:1", "expired");
        verify(zSetOperations, never()).remove("RT_SESSIONS:1", "s1");
    }

    @Test
    void should_evictOldestSession_when_sessionCountExceedsLimit() {
        // given
        Set<String> sessionIds = orderedSet("s0", "s1", "s2", "s3", "s4", "s5");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.range("RT_SESSIONS:1", 0, -1)).willReturn(sessionIds);
        given(redisTemplate.hasKey(anyString())).willReturn(true);

        // when
        tokenRepository.saveSession(1L, "s5", "token", TTL);

        // then
        verify(redisTemplate).delete("RT:1:s0");
        verify(zSetOperations).remove("RT_SESSIONS:1", "s0");
        verify(redisTemplate, never()).delete("RT:1:s1");
    }

    @Test
    void should_returnRotated_when_storedTokenMatches() {
        // given
        givenRotateScriptReturns(1L);

        // when
        RotationResult result = tokenRepository.rotate(1L, "s1", "old", "new", TTL);

        // then
        assertThat(result).isEqualTo(RotationResult.ROTATED);
    }

    @Test
    void should_returnReuseDetected_when_storedTokenDoesNotMatch() {
        // given — 스크립트가 불일치 시 세션을 폐기하고 0을 반환
        givenRotateScriptReturns(0L);

        // when
        RotationResult result = tokenRepository.rotate(1L, "s1", "old", "new", TTL);

        // then
        assertThat(result).isEqualTo(RotationResult.REUSE_DETECTED);
    }

    @Test
    void should_returnSessionNotFound_when_sessionDoesNotExist() {
        // given
        givenRotateScriptReturns(-1L);

        // when
        RotationResult result = tokenRepository.rotate(1L, "s1", "old", "new", TTL);

        // then
        assertThat(result).isEqualTo(RotationResult.SESSION_NOT_FOUND);
    }

    @Test
    void should_deleteOnlyThatSession_when_deleteSession() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        // when
        tokenRepository.deleteSession(1L, "s1");

        // then
        verify(redisTemplate).delete("RT:1:s1");
        verify(zSetOperations).remove("RT_SESSIONS:1", "s1");
    }

    @Test
    void should_deleteEverySessionAndList_when_deleteAllSessions() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.range("RT_SESSIONS:1", 0, -1)).willReturn(orderedSet("s1", "s2"));

        // when
        tokenRepository.deleteAllSessions(1L);

        // then
        verify(redisTemplate).delete(List.of("RT:1:s1", "RT:1:s2", "RT_SESSIONS:1"));
    }

    private void givenRotateScriptReturns(Long scriptResult) {
        given(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("RT:1:s1", "RT_SESSIONS:1")),
                eq("old"), eq("new"), eq(String.valueOf(TTL)), eq("s1"))).willReturn(scriptResult);
    }

    private static Set<String> orderedSet(String... values) {
        return new LinkedHashSet<>(List.of(values));
    }
}
