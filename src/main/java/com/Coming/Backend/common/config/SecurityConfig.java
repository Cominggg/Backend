package com.Coming.Backend.common.config;

import com.Coming.Backend.auth.jwt.JwtAuthenticationFilter;
import com.Coming.Backend.auth.jwt.JwtProvider;
import com.Coming.Backend.auth.oauth2.CustomOAuth2UserService;
import com.Coming.Backend.auth.oauth2.OAuth2FailureHandler;
import com.Coming.Backend.auth.oauth2.OAuth2SuccessHandler;
import com.Coming.Backend.auth.repository.BlacklistRepository;
import com.Coming.Backend.common.discord.DiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.filter.MdcLoggingFilter;
import com.Coming.Backend.common.filter.RateLimitFilter;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    private final JwtProvider jwtProvider;
    private final BlacklistRepository blacklistRepository;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final ProxyManager<String> rateLimitProxyManager;
    private final DiscordNotifier discordNotifier;

    public SecurityConfig(JwtProvider jwtProvider, BlacklistRepository blacklistRepository,
            CustomOAuth2UserService oAuth2UserService, OAuth2SuccessHandler oAuth2SuccessHandler,
            OAuth2FailureHandler oAuth2FailureHandler, ObjectMapper objectMapper,
            Environment environment, ProxyManager<String> rateLimitProxyManager,
            DiscordNotifier discordNotifier) {
        this.jwtProvider = jwtProvider;
        this.blacklistRepository = blacklistRepository;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.rateLimitProxyManager = rateLimitProxyManager;
        this.discordNotifier = discordNotifier;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'; frame-ancestors 'none';"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentTypeOptions(contentType -> {})
                        .frameOptions(frame -> frame.deny())
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    if (environment.acceptsProfiles(Profiles.of("local"))) {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/dev/**").permitAll();
                    }
                    auth
                            .requestMatchers(
                                    "/api/auth/login/**",
                                    "/api/auth/callback/**",
                                    "/api/auth/refresh",
                                    "/api/auth/check-nickname",
                                    "/actuator/health"
                            ).permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/artists/following").hasAnyRole("USER", "ADMIN")
                            .requestMatchers(HttpMethod.GET, "/api/concerts/following").hasAnyRole("USER", "ADMIN")
                            .requestMatchers(HttpMethod.GET,
                                    "/api/artists/**",
                                    "/api/concerts/**",
                                    "/api/releases/**",
                                    "/api/calendar"
                            ).permitAll()
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/auth/register").hasRole("PENDING")
                            .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole("USER", "ADMIN", "PENDING")
                            .requestMatchers("/api/auth/logout", "/api/auth/withdraw").hasAnyRole("USER", "ADMIN", "PENDING")
                            .anyRequest().hasAnyRole("USER", "ADMIN");
                })
                .oauth2Login(oauth2 -> oauth2
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/callback/*"))
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            log.warn("[{}] 401 UNAUTHORIZED — {} {}",
                                    MDC.get("traceId"), request.getMethod(), request.getRequestURI());
                            discordNotifier.notifyFourXx(request, ErrorCode.UNAUTHORIZED);
                            writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            discordNotifier.notifyFourXx(request, ErrorCode.FORBIDDEN);
                            writeErrorResponse(response, ErrorCode.FORBIDDEN);
                        })
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, blacklistRepository),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        new RateLimitFilter(rateLimitProxyManager, objectMapper, discordNotifier),
                        JwtAuthenticationFilter.class
                )
                .addFilterBefore(
                        new MdcLoggingFilter(),
                        RateLimitFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeErrorResponse(jakarta.servlet.http.HttpServletResponse response,
            ErrorCode errorCode) throws java.io.IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", errorCode.name(),
                "message", errorCode.getMessage()
        ));
    }
}
