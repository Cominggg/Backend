package com.Coming.Backend.admin.client;

import com.Coming.Backend.admin.dto.DataArtistSearchResult;
import com.Coming.Backend.admin.dto.DataConcertSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

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
                .block();
    }

    /**
     * Data 파이프라인에 특정 공연의 셋리스트 수집을 트리거한다.
     */
    public void triggerConcertSetlist(Long concertId) {
        webClient.post()
                .uri("/collect/concert/{id}/setlist", concertId)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.info("Data pipeline setlist collect triggered: concertId={}", concertId))
                .doOnError(e -> log.warn("Data pipeline setlist collect failed: concertId={}, error={}", concertId, e.getMessage()))
                .block();
    }
}
