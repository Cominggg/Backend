package com.Coming.Backend.common.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(120);

    /**
     * WebClient.Builder는 변경 가능한 객체라, 주입받는 곳마다 새 인스턴스를 주어 한 클라이언트의
     * baseUrl·defaultHeader(예: 내부 시크릿)가 다른 클라이언트에 섞이지 않게 한다.
     */
    @Bean
    @Scope("prototype")
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                .responseTimeout(RESPONSE_TIMEOUT);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
