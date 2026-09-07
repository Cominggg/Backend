package com.Coming.Backend.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
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
class ArtistCollectLockRepositoryTest {

    @InjectMocks
    private ArtistCollectLockRepository artistCollectLockRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void should_return_true_when_try_lock_with_unheld_mbid() {
        // given
        String mbid = "some-mbid-123";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("ARTIST_COLLECT_LOCK:" + mbid, "1", 150, TimeUnit.SECONDS))
                .willReturn(true);

        // when
        boolean result = artistCollectLockRepository.tryLock(mbid);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void should_return_false_when_try_lock_with_already_held_mbid() {
        // given
        String mbid = "some-mbid-123";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent("ARTIST_COLLECT_LOCK:" + mbid, "1", 150, TimeUnit.SECONDS))
                .willReturn(false);

        // when
        boolean result = artistCollectLockRepository.tryLock(mbid);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void should_delete_key_when_unlock_called() {
        // given
        String mbid = "some-mbid-123";

        // when
        artistCollectLockRepository.unlock(mbid);

        // then
        verify(redisTemplate).delete("ARTIST_COLLECT_LOCK:" + mbid);
    }
}
