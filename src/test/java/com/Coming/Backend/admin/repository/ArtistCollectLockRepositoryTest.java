package com.Coming.Backend.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class ArtistCollectLockRepositoryTest {

    @InjectMocks
    private ArtistCollectLockRepository artistCollectLockRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void should_return_generated_token_when_try_lock_with_unheld_mbid() {
        // given
        String mbid = "some-mbid-123";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
                eq("ARTIST_COLLECT_LOCK:" + mbid), any(), eq(150L), eq(TimeUnit.SECONDS)))
                .willReturn(true);

        // when
        String token = artistCollectLockRepository.tryLock(mbid);

        // then
        assertThat(token).isNotNull();
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(
                eq("ARTIST_COLLECT_LOCK:" + mbid), tokenCaptor.capture(),
                eq(150L), eq(TimeUnit.SECONDS));
        assertThat(tokenCaptor.getValue()).isEqualTo(token);
    }

    @Test
    void should_return_null_when_try_lock_with_already_held_mbid() {
        // given
        String mbid = "some-mbid-123";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
                eq("ARTIST_COLLECT_LOCK:" + mbid), any(), eq(150L), eq(TimeUnit.SECONDS)))
                .willReturn(false);

        // when
        String token = artistCollectLockRepository.tryLock(mbid);

        // then
        assertThat(token).isNull();
    }

    @Test
    void should_execute_unlock_script_with_key_and_token_when_unlock_called() {
        // given
        String mbid = "some-mbid-123";
        String token = "token-abc";

        // when
        artistCollectLockRepository.unlock(mbid, token);

        // then
        verify(redisTemplate).execute(
                any(RedisScript.class), eq(List.of("ARTIST_COLLECT_LOCK:" + mbid)), eq(token));
    }
}
