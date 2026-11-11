package com.Coming.Backend.common.util;

import com.Coming.Backend.common.exception.ContentInvalidFormatException;
import com.Coming.Backend.common.exception.ContentTooLongException;
import com.Coming.Backend.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TiptapContentCodecTest {

    private static final String SAMPLE_JSON =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"안녕하세요\"}]}]}";

    /**
     * text 텍스트 노드 하나를 가진 Tiptap 문서. 요청 바디를 역직렬화한 것과 같은 Map/List 형태다.
     */
    private static Map<String, Object> docWithText(String text) {
        return Map.of("type", "doc", "content", List.of(
                Map.of("type", "paragraph", "content", List.of(
                        Map.of("type", "text", "text", text)
                ))
        ));
    }

    /**
     * 텍스트 없이 빈 문단만 count개 둔 Tiptap 문서. 텍스트 길이 검사는 통과하고 JSON만 커지게 만든다.
     */
    private static Map<String, Object> docWithEmptyParagraphs(int count) {
        List<Object> paragraphs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            paragraphs.add(Map.of("type", "paragraph"));
        }
        return Map.of("type", "doc", "content", paragraphs);
    }

    // ===== sanitize =====

    @Test
    void should_return_tree_json_and_text_when_valid_content_given() {
        // given
        Map<String, Object> content = docWithText("안녕하세요");

        // when
        TiptapContentCodec.SanitizedContent result = TiptapContentCodec.sanitize(content);

        // then
        assertThat(result.tree()).isEqualTo(content);
        assertThat(result.json()).isEqualTo(SAMPLE_JSON);
        assertThat(result.text()).isEqualTo("안녕하세요");
    }

    @Test
    void should_return_json_without_disallowed_attrs_when_content_has_paragraph_text_align() {
        // given
        Map<String, Object> content = Map.of("type", "doc", "content", List.of(
                Map.of("type", "paragraph", "attrs", Map.of("textAlign", "center"), "content", List.of(
                        Map.of("type", "text", "text", "안녕하세요")
                ))
        ));

        // when
        TiptapContentCodec.SanitizedContent result = TiptapContentCodec.sanitize(content);

        // then
        assertThat(result.json()).isEqualTo(SAMPLE_JSON);
    }

    @Test
    void should_throw_content_invalid_format_exception_when_content_has_disallowed_node() {
        // given
        Map<String, Object> content = Map.of("type", "doc", "content", List.of(
                Map.of("type", "image", "attrs", Map.of("src", "https://example.com/a.png"))
        ));

        // when & then
        assertThatThrownBy(() -> TiptapContentCodec.sanitize(content))
                .isInstanceOf(ContentInvalidFormatException.class)
                .hasMessage(ErrorCode.CONTENT_INVALID_FORMAT.getMessage());
    }

    @Test
    void should_throw_content_too_long_exception_when_text_exceeds_10000() {
        // given
        Map<String, Object> content = docWithText("a".repeat(10001));

        // when & then
        assertThatThrownBy(() -> TiptapContentCodec.sanitize(content))
                .isInstanceOf(ContentTooLongException.class)
                .hasMessage(ErrorCode.CONTENT_TOO_LONG.getMessage());
    }

    @Test
    void should_return_sanitized_content_when_text_is_exactly_10000() {
        // given
        Map<String, Object> content = docWithText("a".repeat(10000));

        // when
        TiptapContentCodec.SanitizedContent result = TiptapContentCodec.sanitize(content);

        // then
        assertThat(result.text()).hasSize(10000);
    }

    @Test
    void should_throw_content_too_long_exception_when_json_exceeds_50000_with_short_text() {
        // given
        // {"type":"paragraph"} 3,000개는 JSON 약 63,000자, 텍스트는 0자
        Map<String, Object> content = docWithEmptyParagraphs(3000);

        // when & then
        assertThatThrownBy(() -> TiptapContentCodec.sanitize(content))
                .isInstanceOf(ContentTooLongException.class)
                .hasMessage(ErrorCode.CONTENT_TOO_LONG.getMessage());
    }

    // ===== read =====

    @Test
    void should_return_original_tree_when_reading_sanitized_json() {
        // given
        Map<String, Object> content = docWithText("안녕하세요");
        String json = TiptapContentCodec.sanitize(content).json();

        // when
        Object result = TiptapContentCodec.read(json);

        // then
        assertThat(result).isEqualTo(content);
    }

    @Test
    void should_throw_illegal_state_exception_when_reading_invalid_json() {
        // given
        String json = "점검 안내";

        // when & then
        assertThatThrownBy(() -> TiptapContentCodec.read(json))
                .isInstanceOf(IllegalStateException.class);
    }
}
