package com.Coming.Backend.auth.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class SuspensionNoticeTemplateTest {

    private TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private Context context(String detail) {
        Context context = new Context();
        context.setVariable("period", "2026년 11월 5일 12:00까지");
        context.setVariable("reasonLabel", "스팸/광고");
        context.setVariable("detail", detail);
        context.setVariable("operationPolicyUrl", "https://www.comingg.com/operation-policy");
        context.setVariable("fromAddress", "coming@example.com");
        context.setVariable("logoContentId", "coming-logo");
        return context;
    }

    @Test
    void should_render_period_reason_detail_and_appeal_guide_when_detail_given() {
        // when
        String html = templateEngine().process("mail/suspension-notice", context("같은 글 반복 게시"));

        // then
        assertThat(html).contains("2026년 11월 5일 12:00까지", "스팸/광고", "같은 글 반복 게시",
                "7일 이내", "coming@example.com", "https://www.comingg.com/operation-policy");
    }

    @Test
    void should_omit_detail_row_when_detail_is_null() {
        // when
        String html = templateEngine().process("mail/suspension-notice", context(null));

        // then
        assertThat(html).doesNotContain("상세 사유");
    }
}
