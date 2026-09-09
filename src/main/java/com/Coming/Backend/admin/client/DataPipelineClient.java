package com.Coming.Backend.admin.client;

import com.Coming.Backend.admin.dto.DataArtistSearchResult;
import com.Coming.Backend.admin.dto.DataConcertSearchResult;
import com.Coming.Backend.admin.dto.PipelineArtistCollectResult;
import com.Coming.Backend.admin.dto.PipelineConcertCollectResult;
import com.Coming.Backend.admin.dto.PipelineSetlistCollectResult;
import com.Coming.Backend.admin.exception.PipelineConflictException;
import com.Coming.Backend.admin.exception.PipelineNotFoundException;
import com.Coming.Backend.admin.exception.PipelineServerException;
import com.Coming.Backend.admin.exception.PipelineTimeoutException;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DataPipelineClient {

    private final WebClient webClient;

    public DataPipelineClient(
            WebClient.Builder builder,
            @Value("${data-pipeline.base-url}") String baseUrl,
            @Value("${data-pipeline.secret}") String secret) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Secret", secret)
                .build();
    }

    /**
     * MusicBrainz에서 아티스트명으로 후보를 검색한다. 최대 10건 반환.
     */
    public List<DataArtistSearchResult> searchArtists(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/artists").queryParam("name", name).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<DataArtistSearchResult>>() {})
                .doOnError(e -> log.warn("Data pipeline artist search failed: name={}, error={}", name, e.getMessage()))
                .onErrorMap(DataPipelineClient::isTimeout, e -> new PipelineTimeoutException())
                .block();
    }

    /**
     * KOPIS에서 공연명으로 후보를 검색한다. 오늘~2년 후 범위, 최대 20건 반환.
     */
    public List<DataConcertSearchResult> searchConcerts(String title) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/concerts").queryParam("title", title).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<DataConcertSearchResult>>() {})
                .doOnError(e -> log.warn("Data pipeline concert search failed: title={}, error={}", title, e.getMessage()))
                .onErrorMap(DataPipelineClient::isTimeout, e -> new PipelineTimeoutException())
                .block();
    }

    /**
     * MBID 기반으로 아티스트를 동기 수집한다. 수집 결과를 반환한다.
     *
     * @throws PipelineNotFoundException MusicBrainz에 해당 MBID가 없는 경우
     * @throws PipelineConflictException 동일 MBID에 대한 수집이 이미 처리 중인 경우
     * @throws PipelineTimeoutException  Data 파이프라인 응답이 설정된 시간 내에 오지 않은 경우
     */
    public PipelineArtistCollectResult collectArtist(String mbid) {
        return webClient.post()
                .uri("/collect/artist")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("mbid", mbid))
                .retrieve()
                .onStatus(status -> status.value() == 404, r -> Mono.error(new PipelineNotFoundException()))
                .onStatus(status -> status.value() == 409, r -> Mono.error(new PipelineConflictException()))
                .onStatus(status -> status.is5xxServerError(), r -> Mono.error(new PipelineServerException()))
                .bodyToMono(PipelineArtistCollectResult.class)
                .doOnSuccess(r -> log.info("Artist collect completed: mbid={}, success={}", mbid, r.success()))
                .doOnError(e -> log.warn("Artist collect failed: mbid={}, error={}", mbid, e.getMessage()))
                .onErrorMap(DataPipelineClient::isTimeout, e -> new PipelineTimeoutException())
                .block();
    }

    /**
     * KOPIS ID 기반으로 공연을 동기 수집한다. 수집 결과를 반환한다.
     *
     * @throws PipelineNotFoundException KOPIS에 해당 ID가 없는 경우
     * @throws PipelineConflictException 동일 KOPIS ID에 대한 수집이 이미 처리 중인 경우
     * @throws PipelineTimeoutException  Data 파이프라인 응답이 설정된 시간 내에 오지 않은 경우
     */
    public PipelineConcertCollectResult collectConcert(String kopisId) {
        return webClient.post()
                .uri("/collect/concert")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("kopis_id", kopisId))
                .retrieve()
                .onStatus(status -> status.value() == 404, r -> Mono.error(new PipelineNotFoundException()))
                .onStatus(status -> status.value() == 409, r -> Mono.error(new PipelineConflictException()))
                .onStatus(status -> status.is5xxServerError(), r -> Mono.error(new PipelineServerException()))
                .bodyToMono(PipelineConcertCollectResult.class)
                .doOnSuccess(r -> log.info("Concert collect completed: kopisId={}, success={}", kopisId, r.success()))
                .doOnError(e -> log.warn("Concert collect failed: kopisId={}, error={}", kopisId, e.getMessage()))
                .onErrorMap(DataPipelineClient::isTimeout, e -> new PipelineTimeoutException())
                .block();
    }

    /**
     * Data 파이프라인에 특정 아티스트의 릴리즈 수집을 트리거한다.
     */
    public void triggerArtistReleases(Long artistId) {
        webClient.post()
                .uri("/collect/artist/{id}/releases", artistId)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.info("Data pipeline artist releases triggered: artistId={}", artistId))
                .doOnError(e -> log.warn("Data pipeline artist releases failed: artistId={}, error={}", artistId, e.getMessage()))
                .onErrorMap(DataPipelineClient::isTimeout, e -> new PipelineTimeoutException())
                .block();
    }

    /**
     * 특정 공연의 셋리스트를 동기 수집한다. 수집 결과를 반환한다.
     *
     * @throws PipelineNotFoundException setlist.fm에 해당 공연의 셋리스트가 없는 경우
     * @throws PipelineConflictException 동일 공연에 대한 수집이 이미 처리 중인 경우
     * @throws PipelineTimeoutException  Data 파이프라인 응답이 설정된 시간 내에 오지 않은 경우
     */
    public PipelineSetlistCollectResult collectConcertSetlist(Long concertId) {
        return webClient.post()
                .uri("/collect/concert/{id}/setlist", concertId)
                .retrieve()
                .onStatus(status -> status.value() == 404, r -> Mono.error(new PipelineNotFoundException()))
                .onStatus(status -> status.value() == 409, r -> Mono.error(new PipelineConflictException()))
                .onStatus(status -> status.is5xxServerError(), r -> Mono.error(new PipelineServerException()))
                .bodyToMono(PipelineSetlistCollectResult.class)
                .doOnSuccess(r -> log.info("Setlist collect completed: concertId={}, success={}", concertId, r.success()))
                .doOnError(e -> log.warn("Setlist collect failed: concertId={}, error={}", concertId, e.getMessage()))
                .onErrorMap(DataPipelineClient::isTimeout, e -> new PipelineTimeoutException())
                .block();
    }

    // WebClient의 connectTimeout/responseTimeout 초과는 각각 ConnectTimeoutException,
    // ReadTimeoutException(io.netty.handler.timeout.TimeoutException)으로 전달되며,
    // 둘 다 원본 예외를 감싸는 WebClientRequestException의 cause로 담겨온다.
    private static boolean isTimeout(Throwable e) {
        Throwable cause = e.getCause();
        return cause instanceof TimeoutException || cause instanceof ConnectTimeoutException;
    }
}
