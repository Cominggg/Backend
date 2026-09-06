package com.Coming.Backend.admin.client;

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
        server.start();

        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(50));
        WebClient.Builder builder = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
        dataPipelineClient = new DataPipelineClient(builder, "http://localhost:" + server.getAddress().getPort(), "secret");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void should_throwPipelineTimeoutException_when_responseExceedsConfiguredTimeout() {
        // when
        // then
        assertThatThrownBy(() -> dataPipelineClient.searchArtists("test"))
                .isInstanceOf(PipelineTimeoutException.class);
    }
}
