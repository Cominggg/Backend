package com.Coming.Backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TokenRepositoryTest {

    @InjectMocks
    private TokenRepository tokenRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void should_return_token_when_find_after_save() {
        // given
        Long userId = 1L;
        String refreshToken = "refresh-token-value";
        long ttlMillis = 604_800_000L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:1")).willReturn(refreshToken);

        // when
        tokenRepository.save(userId, refreshToken, ttlMillis);
        Optional<String> result = tokenRepository.find(userId);

        // then
        assertThat(result).isPresent().contains(refreshToken);
        verify(valueOperations).set("RT:1", refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Test
    void should_return_empty_when_find_after_delete() {
        // given
        Long userId = 1L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:1")).willReturn(null);
        given(redisTemplate.delete("RT:1")).willReturn(true);

        // when
        tokenRepository.delete(userId);
        Optional<String> result = tokenRepository.find(userId);

        // then
        assertThat(result).isEmpty();
        verify(redisTemplate).delete("RT:1");
    }

    @Test
    void should_return_empty_when_find_with_unsaved_userId() {
        // given
        Long userId = 999L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:999")).willReturn(null);

        // when
        Optional<String> result = tokenRepository.find(userId);

        // then
        assertThat(result).isEmpty();
    }
}
