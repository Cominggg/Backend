package com.Coming.Backend.app.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Coming.Backend.app.dto.AppVersionResponse;
import com.Coming.Backend.app.entity.AppPlatform;
import com.Coming.Backend.app.service.AppVersionService;
import com.Coming.Backend.common.discord.NoOpDiscordNotifier;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AppVersionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppVersionService appVersionService;

    @InjectMocks
    private AppVersionController appVersionController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(appVersionController)
                .setControllerAdvice(new GlobalExceptionHandler(new NoOpDiscordNotifier()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .build();
    }

    @Test
    void should_return_android_min_version_code_when_platform_is_android() throws Exception {
        // given
        given(appVersionService.getMinVersion(AppPlatform.ANDROID)).willReturn(new AppVersionResponse(10000));

        // when & then
        mockMvc.perform(get("/api/app/version").param("platform", "ANDROID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minVersionCode").value(10000));
    }

    @Test
    void should_return_ios_min_version_code_when_platform_is_ios() throws Exception {
        // given
        given(appVersionService.getMinVersion(AppPlatform.IOS)).willReturn(new AppVersionResponse(0));

        // when & then
        mockMvc.perform(get("/api/app/version").param("platform", "IOS").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minVersionCode").value(0));
    }

    @Test
    void should_return_400_when_platform_is_unsupported() throws Exception {
        // given
        String platform = "WEB";

        // when & then
        mockMvc.perform(get("/api/app/version").param("platform", platform).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());

        // then
        verifyNoInteractions(appVersionService);
    }

    @Test
    void should_return_400_when_platform_is_lowercase() throws Exception {
        // given
        String platform = "android";

        // when & then
        mockMvc.perform(get("/api/app/version").param("platform", platform).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));

        // then
        verifyNoInteractions(appVersionService);
    }

    @Test
    void should_return_400_when_platform_is_missing() throws Exception {
        // given: platform 파라미터 없음

        // when & then
        mockMvc.perform(get("/api/app/version").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()))
                .andExpect(jsonPath("$.message").exists());

        // then
        verifyNoInteractions(appVersionService);
    }
}
