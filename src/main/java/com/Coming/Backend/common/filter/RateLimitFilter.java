package com.Coming.Backend.common.filter;

import com.Coming.Backend.common.discord.DiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.BandwidthBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "RL:";

    // 버킷 용량 20, 초당 10개 refill — 순간 burst 허용하되 지속적 과부하 차단
    private static final BucketConfiguration BUCKET_CONFIG = BucketConfiguration.builder()
            .addLimit(BandwidthBuilder.builder()
                    .capacity(20)
                    .refillGreedy(10, Duration.ofSeconds(1))
                    .build())
            .build();

    private final ProxyManager<String> proxyManager;
    private final ObjectMapper objectMapper;
    private final DiscordNotifier discordNotifier;

    public RateLimitFilter(ProxyManager<String> proxyManager, ObjectMapper objectMapper,
            DiscordNotifier discordNotifier) {
        this.proxyManager = proxyManager;
        this.objectMapper = objectMapper;
        this.discordNotifier = discordNotifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String ip = extractClientIp(request);
        Bucket bucket = proxyManager.builder().build(KEY_PREFIX + ip, () -> BUCKET_CONFIG);

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded: ip={}", ip);
            discordNotifier.notifyFourXx(request, ErrorCode.RATE_LIMIT_EXCEEDED);
            writeErrorResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    // 루프백·사설 IP(Nginx 등 내부 프록시)에서 온 경우에만 X-Forwarded-For 신뢰
    private boolean isTrustedProxy(String addr) {
        return addr.equals("127.0.0.1")
                || addr.equals("0:0:0:0:0:0:0:1")
                || addr.startsWith("10.")
                || addr.startsWith("192.168.")
                || (addr.startsWith("172.") && isTrustedPrivate172(addr));
    }

    private boolean isTrustedPrivate172(String addr) {
        try {
            int second = Integer.parseInt(addr.split("\\.")[1]);
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                "message", ErrorCode.RATE_LIMIT_EXCEEDED.getMessage()
        ));
    }
}
