package com.Coming.Backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class BlacklistRepositoryTest {

    @InjectMocks
    private BlacklistRepository blacklistRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void should_return_true_when_isBlacklisted_after_save() {
        // given
        String accessToken = "valid-access-token";
        long remainingExpiryMillis = 1_800_000L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.hasKey("BL:" + accessToken)).willReturn(true);

        // when
        blacklistRepository.save(accessToken, remainingExpiryMillis);
        boolean result = blacklistRepository.isBlacklisted(accessToken);

        // then
        assertThat(result).isTrue();
        verify(valueOperations).set("BL:" + accessToken, "1", remainingExpiryMillis, TimeUnit.MILLISECONDS);
    }

    @Test
    void should_return_false_when_isBlacklisted_with_unsaved_token() {
        // given
        String accessToken = "not-blacklisted-token";

        given(redisTemplate.hasKey("BL:" + accessToken)).willReturn(false);

        // when
        boolean result = blacklistRepository.isBlacklisted(accessToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void should_not_call_set_when_remainingExpiryMillis_is_zero() {
        // given
        String accessToken = "expired-token";
        long remainingExpiryMillis = 0L;

        // when
        blacklistRepository.save(accessToken, remainingExpiryMillis);

        // then
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void should_not_call_set_when_remainingExpiryMillis_is_negative() {
        // given
        String accessToken = "expired-token";
        long remainingExpiryMillis = -1L;

        // when
        blacklistRepository.save(accessToken, remainingExpiryMillis);

        // then
        verify(redisTemplate, never()).opsForValue();
    }
}
