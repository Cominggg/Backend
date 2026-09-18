package com.Coming.Backend.policy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.common.discord.NoOpDiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import com.Coming.Backend.policy.dto.PolicyRegisterRequest;
import com.Coming.Backend.policy.dto.PolicyResponse;
import com.Coming.Backend.policy.entity.PolicyType;
import com.Coming.Backend.policy.exception.PolicyVersionDuplicateException;
import com.Coming.Backend.policy.service.PolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PolicyService policyService;

    @InjectMocks
    private PolicyController policyController;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(policyController)
                .setControllerAdvice(new GlobalExceptionHandler(new NoOpDiscordNotifier()))
                .setValidator(validator)
                .build();
    }

    private PolicyRegisterRequest validRequest() {
        return new PolicyRegisterRequest(
                PolicyType.TERMS,
                "1.0.0",
                LocalDate.of(2026, 1, 1),
                "이용약관 개정",
                "https://coming.example.com/policies/terms/1.0.0"
        );
    }

    @Test
    void should_return_201_when_register_request_is_valid() throws Exception {
        // given
        PolicyRegisterRequest request = validRequest();
        PolicyResponse response = new PolicyResponse(
                1L, PolicyType.TERMS, "1.0.0", LocalDate.of(2026, 1, 1),
                "이용약관 개정", "https://coming.example.com/policies/terms/1.0.0");
        given(policyService.registerPolicy(any(PolicyRegisterRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.type").value("TERMS"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.effectiveDate").value("2026-01-01"))
                .andExpect(jsonPath("$.changeSummary").value("이용약관 개정"))
                .andExpect(jsonPath("$.detailUrl").value("https://coming.example.com/policies/terms/1.0.0"));
    }

    @Test
    void should_return_400_when_type_is_null_on_register() throws Exception {
        // given
        PolicyRegisterRequest request = new PolicyRegisterRequest(
                null, "1.0.0", LocalDate.of(2026, 1, 1),
                "이용약관 개정", "https://coming.example.com/policies/terms/1.0.0");

        // when & then
        mockMvc.perform(post("/api/admin/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_400_when_version_exceeds_max_length_on_register() throws Exception {
        // given
        PolicyRegisterRequest request = new PolicyRegisterRequest(
                PolicyType.TERMS, "1".repeat(51), LocalDate.of(2026, 1, 1),
                "이용약관 개정", "https://coming.example.com/policies/terms/1.0.0");

        // when & then
        mockMvc.perform(post("/api/admin/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void should_return_409_when_policy_version_already_registered() throws Exception {
        // given
        PolicyRegisterRequest request = validRequest();
        given(policyService.registerPolicy(any(PolicyRegisterRequest.class)))
                .willThrow(new PolicyVersionDuplicateException());

        // when & then
        mockMvc.perform(post("/api/admin/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.POLICY_VERSION_DUPLICATE.name()))
                .andExpect(jsonPath("$.message").exists());
    }
}
