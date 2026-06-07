package com.Coming.Backend.admin.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
     * Data 파이프라인에 KOPIS 공연 수집을 트리거한다.
     */
    public void triggerConcertCollect() {
        webClient.post()
                .uri("/collect/concert")
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.info("Data pipeline concert collect triggered"))
                .doOnError(e -> log.warn("Data pipeline concert collect failed: {}", e.getMessage()))
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
