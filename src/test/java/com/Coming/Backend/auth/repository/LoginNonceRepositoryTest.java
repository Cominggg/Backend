package com.Coming.Backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class LoginNonceRepositoryTest {

    @InjectMocks
    private LoginNonceRepository loginNonceRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String NONCE = "q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6j7k8l9z0x1c";

    @Test
    void should_store_nonce_with_prefix_and_ttl_when_save() {
        // given
        Duration ttl = Duration.ofMinutes(5);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        loginNonceRepository.save(NONCE, ttl);

        // then
        verify(valueOperations).set("NONCE:" + NONCE, "1", ttl);
    }

    @Test
    void should_return_true_when_issued_nonce_is_deleted() {
        // given
        given(redisTemplate.delete("NONCE:" + NONCE)).willReturn(Boolean.TRUE);

        // when
        boolean result = loginNonceRepository.consume(NONCE);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void should_return_false_when_nonce_does_not_exist_or_was_already_consumed() {
        // given
        given(redisTemplate.delete("NONCE:" + NONCE)).willReturn(Boolean.FALSE);

        // when
        boolean result = loginNonceRepository.consume(NONCE);

        // then
        assertThat(result).isFalse();
    }
}
