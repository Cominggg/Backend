package com.Coming.Backend.concert.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.concert.dto.SetlistResponse;
import com.Coming.Backend.concert.dto.SetlistTrackDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SetlistCacheRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private SetlistCacheRepository setlistCacheRepository;

    private static final String SETLIST_FM_ID = "63de4613";
    private static final String KEY = "SETLIST_FM:" + SETLIST_FM_ID;
    private static final SetlistResponse SETLIST = new SetlistResponse(
            List.of(new SetlistTrackDto(1, "Twist and Shout"), new SetlistTrackDto(2, "Long Tall Sally")),
            "https://www.setlist.fm/setlist/the-beatles/1964/hollywood-bowl-los-angeles-ca-63de4613.html");

    @BeforeEach
    void setUp() {
        setlistCacheRepository = new SetlistCacheRepository(redisTemplate, objectMapper);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    void should_store_json_with_prefixed_key_and_12h_ttl_when_save() {
        // given
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);

        // when
        setlistCacheRepository.save(SETLIST_FM_ID, SETLIST);

        // then
        verify(valueOperations).set(eq(KEY), json.capture(), eq(SetlistCacheRepository.TTL));
        assertThat(objectMapper.readValue(json.getValue(), SetlistResponse.class)).isEqualTo(SETLIST);
    }

    @Test
    void should_return_deserialized_setlist_when_cached_value_exists() {
        // given
        given(valueOperations.get(KEY)).willReturn(objectMapper.writeValueAsString(SETLIST));

        // when
        Optional<SetlistResponse> result = setlistCacheRepository.find(SETLIST_FM_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().tracks())
                .extracting(SetlistTrackDto::order, SetlistTrackDto::title)
                .containsExactly(
                        tuple(1, "Twist and Shout"),
                        tuple(2, "Long Tall Sally"));
        assertThat(result.get().sourceUrl()).isEqualTo(SETLIST.sourceUrl());
    }

    @Test
    void should_return_null_source_url_when_cached_setlist_has_no_source_url() {
        // given
        SetlistResponse withoutUrl = new SetlistResponse(List.of(new SetlistTrackDto(1, "Twist and Shout")), null);
        given(valueOperations.get(KEY)).willReturn(objectMapper.writeValueAsString(withoutUrl));

        // when
        Optional<SetlistResponse> result = setlistCacheRepository.find(SETLIST_FM_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().sourceUrl()).isNull();
    }

    @Test
    void should_return_empty_when_cached_value_does_not_exist() {
        // given
        given(valueOperations.get(KEY)).willReturn(null);

        // when
        Optional<SetlistResponse> result = setlistCacheRepository.find(SETLIST_FM_ID);

        // then
        assertThat(result).isEmpty();
    }
}
