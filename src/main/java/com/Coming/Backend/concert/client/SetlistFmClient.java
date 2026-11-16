package com.Coming.Backend.concert.client;

import com.Coming.Backend.concert.dto.SetlistResponse;
import com.Coming.Backend.concert.dto.SetlistTrackDto;
import com.Coming.Backend.concert.exception.SetlistUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * setlist.fm API로 셋리스트를 조회한다.
 *
 * <p>약관(§3)상 셋리스트는 요청 시점에 직접 조회해 바로 내려줘야 하므로 사용자 요청 경로에서 호출된다.
 * 그래서 재시도 없이 짧은 타임아웃만 두고, 실패는 즉시 {@link SetlistUnavailableException}으로 알린다.</p>
 */
@Slf4j
@Component
public class SetlistFmClient {

    static final String BASE_URL = "https://api.setlist.fm/rest/1.0";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    @Autowired
    public SetlistFmClient(WebClient.Builder builder, @Value("${setlist-fm.api-key}") String apiKey) {
        this(builder.baseUrl(BASE_URL).defaultHeader("x-api-key", apiKey).build());
    }

    SetlistFmClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * setlist.fm ID로 셋리스트를 조회한다. 곡 순서는 세트(본공연·앙코르)를 이어 1부터 매긴다.
     *
     * @return setlist.fm에서 삭제된 셋리스트(404)이면 빈 Optional
     * @throws SetlistUnavailableException 404 외 오류 응답(429 포함)·연결 실패·타임아웃인 경우
     */
    public Optional<SetlistResponse> getSetlist(String setlistFmId) {
        try {
            SetlistFmSetlist setlist = webClient.get()
                    .uri("/setlist/{setlistId}", setlistFmId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(SetlistFmSetlist.class)
                    .timeout(TIMEOUT)
                    .onErrorResume(e -> isNotFound(e), e -> Mono.empty())
                    .block();
            return Optional.ofNullable(setlist).map(SetlistFmClient::toResponse);
        } catch (RuntimeException e) {
            log.warn("setlist.fm 조회 실패 — setlistFmId: {}, error: {}", setlistFmId, e.getMessage());
            throw new SetlistUnavailableException();
        }
    }

    private static boolean isNotFound(Throwable e) {
        return e instanceof WebClientResponseException r && r.getStatusCode().value() == HttpStatus.NOT_FOUND.value();
    }

    private static SetlistResponse toResponse(SetlistFmSetlist setlist) {
        List<SetlistTrackDto> tracks = new ArrayList<>();
        if (setlist.sets() != null && setlist.sets().set() != null) {
            for (SetlistFmSetlist.Set set : setlist.sets().set()) {
                if (set.song() == null) {
                    continue;
                }
                for (SetlistFmSetlist.Song song : set.song()) {
                    tracks.add(new SetlistTrackDto(tracks.size() + 1, song.name()));
                }
            }
        }
        return new SetlistResponse(tracks, setlist.url());
    }

    record SetlistFmSetlist(String url, Sets sets) {
        record Sets(List<Set> set) {
        }

        record Set(List<Song> song) {
        }

        record Song(String name) {
        }
    }
}
