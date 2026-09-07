package com.Coming.Backend.admin.client;

import com.Coming.Backend.admin.exception.PipelineConflictException;
import com.Coming.Backend.admin.exception.PipelineNotFoundException;
import com.Coming.Backend.admin.exception.PipelineServerException;
import com.Coming.Backend.admin.exception.PipelineTimeoutException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataPipelineClientTest {

    private HttpServer server;
    private DataPipelineClient dataPipelineClient;

    @BeforeEach
    void setUp() throws IOException {
        // given: 지정된 응답 타임아웃(50ms)보다 오래(300ms) 지연 응답하는 로컬 서버
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/search/artists", exchange -> {
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "[]".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        // given: mbid에 따라 404/409/500을 돌려주는 /collect/artist 엔드포인트
        server.createContext("/collect/artist", exchange -> {
            byte[] rawBody = exchange.getRequestBody().readAllBytes();
            String requestBody = new String(rawBody, StandardCharsets.UTF_8);
            int status = requestBody.contains("not-found-mbid") ? 404
                    : requestBody.contains("conflict-mbid") ? 409
                    : 500;
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();

        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(50));
        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        dataPipelineClient = new DataPipelineClient(builder, baseUrl, "secret");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void should_throw_pipeline_timeout_exception_when_response_exceeds_configured_timeout() {
        // when
        // then
        assertThatThrownBy(() -> dataPipelineClient.searchArtists("test"))
                .isInstanceOf(PipelineTimeoutException.class);
    }

    @Test
    void should_throw_pipeline_not_found_exception_when_data_pipeline_returns_404() {
        // when & then
        assertThatThrownBy(() -> dataPipelineClient.collectArtist("not-found-mbid"))
                .isInstanceOf(PipelineNotFoundException.class);
    }

    @Test
    void should_throw_pipeline_conflict_exception_when_data_pipeline_returns_409() {
        // when & then
        assertThatThrownBy(() -> dataPipelineClient.collectArtist("conflict-mbid"))
                .isInstanceOf(PipelineConflictException.class);
    }

    @Test
    void should_throw_pipeline_server_exception_when_data_pipeline_returns_500() {
        // when & then
        assertThatThrownBy(() -> dataPipelineClient.collectArtist("error-mbid"))
                .isInstanceOf(PipelineServerException.class);
    }
}
