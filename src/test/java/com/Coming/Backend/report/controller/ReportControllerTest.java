package com.Coming.Backend.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.discord.NoOpDiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.report.dto.ReportCreateRequest;
import com.Coming.Backend.report.dto.ReportCreateResponse;
import com.Coming.Backend.report.entity.ReportReason;
import com.Coming.Backend.report.entity.ReportTargetType;
import com.Coming.Backend.report.exception.ReportAlreadyExistsException;
import com.Coming.Backend.report.exception.ReportDetailRequiredException;
import com.Coming.Backend.report.exception.ReportTargetNotFoundException;
import com.Coming.Backend.report.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long REPORT_ID = 100L;
    private static final Long POST_ID = 10L;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setControllerAdvice(new GlobalExceptionHandler(new NoOpDiscordNotifier()))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER_ID, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // POST /api/reports
    // -------------------------------------------------------------------------

    @Test
    void should_return_201_when_create_request_is_valid() throws Exception {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.SPAM, null);
        given(reportService.create(eq(USER_ID), any(ReportCreateRequest.class))).willReturn(new ReportCreateResponse(REPORT_ID));

        // when & then
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(REPORT_ID));
    }

    @Test
    void should_return_400_when_target_type_is_null() throws Exception {
        // given
        String requestJson = """
                {"targetType":null,"targetId":10,"reason":"SPAM","detail":null}
                """;

        // when & then
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
    }

    @Test
    void should_return_400_when_target_id_is_null() throws Exception {
        // given
        String requestJson = """
                {"targetType":"POST","targetId":null,"reason":"SPAM","detail":null}
                """;

        // when & then
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
    }

    @Test
    void should_return_400_when_reason_is_null() throws Exception {
        // given
        String requestJson = """
                {"targetType":"POST","targetId":10,"reason":null,"detail":null}
                """;

        // when & then
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
    }

    @Test
    void should_return_400_when_service_throws_report_detail_required_exception() throws Exception {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.ETC, null);
        given(reportService.create(eq(USER_ID), any(ReportCreateRequest.class))).willThrow(new ReportDetailRequiredException());

        // when & then
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.REPORT_DETAIL_REQUIRED.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_404_when_service_throws_report_target_not_found_exception() throws Exception {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.SPAM, null);
        given(reportService.create(eq(USER_ID), any(ReportCreateRequest.class))).willThrow(new ReportTargetNotFoundException());

        // when & then
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.REPORT_TARGET_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_409_when_service_throws_report_already_exists_exception() throws Exception {
        // given
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.POST, POST_ID, ReportReason.SPAM, null);
        given(reportService.create(eq(USER_ID), any(ReportCreateRequest.class))).willThrow(new ReportAlreadyExistsException());

        // when & then
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.REPORT_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
