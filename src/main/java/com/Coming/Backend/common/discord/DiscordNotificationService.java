package com.Coming.Backend.common.discord;

import com.Coming.Backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Profile("prod")
public class DiscordNotificationService implements DiscordNotifier {

    private static final String COOLDOWN_PREFIX_5XX = "discord:cooldown:5xx:";
    private static final String COOLDOWN_PREFIX_4XX = "discord:cooldown:4xx:";
    private static final Duration COOLDOWN_5XX = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_4XX = Duration.ofMinutes(1);
    private static final int COLOR_5XX = 0xE74C3C;
    private static final int COLOR_4XX = 0xE67E22;
    private static final int MAX_STACK_FRAMES = 5;
    private static final int MAX_FIELD_LENGTH = 1020;

    private final WebClient.Builder webClientBuilder;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${discord.webhook.5xx-url:}")
    private String fiveXxUrl;

    @Value("${discord.webhook.4xx-url:}")
    private String fourXxUrl;

    public DiscordNotificationService(WebClient.Builder webClientBuilder,
            RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void notifyFiveXx(HttpServletRequest request, Exception e) {
        String key = COOLDOWN_PREFIX_5XX + e.getClass().getSimpleName();
        if (!acquireCooldown(key, COOLDOWN_5XX)) return;
        sendAsync(fiveXxUrl, buildFiveXxPayload(request, e));
    }

    @Override
    public void notifyFourXx(HttpServletRequest request, ErrorCode errorCode) {
        String key = COOLDOWN_PREFIX_4XX + errorCode.name();
        if (!acquireCooldown(key, COOLDOWN_4XX)) return;
        sendAsync(fourXxUrl, buildFourXxPayload(request, errorCode));
    }

    private boolean acquireCooldown(String key, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", ttl));
    }

    private void sendAsync(String url, Map<String, Object> payload) {
        if (url == null || url.isBlank()) return;
        try {
            String body = objectMapper.writeValueAsString(payload);
            webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(null, err -> log.warn("Discord 알림 전송 실패: {}", err.getMessage()));
        } catch (Exception ex) {
            log.warn("Discord 페이로드 직렬화 실패", ex);
        }
    }

    private Map<String, Object> buildFiveXxPayload(HttpServletRequest request, Exception e) {
        return embedPayload(
                "🔴 [500] 서버 오류",
                COLOR_5XX,
                List.of(
                        field("경로", request.getMethod() + " " + request.getRequestURI(), true),
                        field("traceId", resolveTraceId(), true),
                        field("userId", resolveUserId(), true),
                        field("원인", truncate(e.getClass().getSimpleName() + ": " + e.getMessage()), false),
                        field("스택", truncate(buildStackTrace(e)), false)
                )
        );
    }

    private Map<String, Object> buildFourXxPayload(HttpServletRequest request, ErrorCode errorCode) {
        return embedPayload(
                "🟡 [" + errorCode.getStatus().value() + "] 이상 접근 감지",
                COLOR_4XX,
                List.of(
                        field("경로", request.getMethod() + " " + request.getRequestURI(), true),
                        field("에러코드", errorCode.name(), true),
                        field("traceId", resolveTraceId(), true),
                        field("userId", resolveUserId(), true)
                )
        );
    }

    private Map<String, Object> embedPayload(String title, int color, List<Map<String, Object>> fields) {
        return Map.of("embeds", List.of(Map.of("title", title, "color", color, "fields", fields)));
    }

    private Map<String, Object> field(String name, String value, boolean inline) {
        String v = (value == null || value.isBlank()) ? "-" : value;
        return Map.of("name", name, "value", v, "inline", inline);
    }

    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "-";
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            return id.toString();
        }
        return "anonymous";
    }

    private String buildStackTrace(Exception e) {
        StackTraceElement[] stack = e.getStackTrace();
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(MAX_STACK_FRAMES, stack.length);
        for (int i = 0; i < limit; i++) {
            sb.append(stack[i]).append("\n");
        }
        return sb.toString().trim();
    }

    private String truncate(String value) {
        if (value == null) return "-";
        return value.length() > MAX_FIELD_LENGTH ? value.substring(0, MAX_FIELD_LENGTH) + "..." : value;
    }
}
