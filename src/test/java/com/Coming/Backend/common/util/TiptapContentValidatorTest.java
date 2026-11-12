package com.Coming.Backend.common.util;

import com.Coming.Backend.common.exception.ContentInvalidFormatException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TiptapContentValidatorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static Map<String, Object> parse(String json) throws Exception {
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
        });
    }

    private static String doc(String... blocks) {
        return "{\"type\":\"doc\",\"content\":[" + String.join(",", blocks) + "]}";
    }

    private static String paragraph(String... inlineNodes) {
        return "{\"type\":\"paragraph\",\"content\":[" + String.join(",", inlineNodes) + "]}";
    }

    private static String heading(String levelJson, String... inlineNodes) {
        return "{\"type\":\"heading\",\"attrs\":{\"level\":" + levelJson + "},\"content\":["
                + String.join(",", inlineNodes) + "]}";
    }

    private static String bulletList(String... items) {
        return "{\"type\":\"bulletList\",\"content\":[" + String.join(",", items) + "]}";
    }

    private static String orderedList(String... items) {
        return "{\"type\":\"orderedList\",\"content\":[" + String.join(",", items) + "]}";
    }

    private static String listItem(String... blocks) {
        return "{\"type\":\"listItem\",\"content\":[" + String.join(",", blocks) + "]}";
    }

    private static String blockquote(String... blocks) {
        return "{\"type\":\"blockquote\",\"content\":[" + String.join(",", blocks) + "]}";
    }

    private static String text(String value) {
        return "{\"type\":\"text\",\"text\":\"" + value + "\"}";
    }

    private static String markedText(String value, String... marks) {
        return "{\"type\":\"text\",\"text\":\"" + value + "\",\"marks\":[" + String.join(",", marks) + "]}";
    }

    private static String mark(String type) {
        return "{\"type\":\"" + type + "\"}";
    }

    private static String link(String hrefJson) {
        return "{\"type\":\"link\",\"attrs\":{\"href\":" + hrefJson + "}}";
    }

    private static String hardBreak() {
        return "{\"type\":\"hardBreak\"}";
    }

    private static String card(String attrsJson) {
        return "{\"type\":\"entityMentionCard\",\"attrs\":{" + attrsJson + "}}";
    }

    private static String concertCard() {
        return card("\"entityType\":\"CONCERT\",\"entityId\":7,\"title\":\"IU 콘서트\"");
    }

    // ===== 허용 =====

    @Test
    void should_return_empty_doc_when_empty_doc_given() throws Exception {
        // given
        Map<String, Object> content = parse(doc());

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(Map.of("type", "doc", "content", List.of()));
    }

    @Test
    void should_return_doc_with_empty_content_when_content_key_missing() throws Exception {
        // given
        Map<String, Object> content = parse("{\"type\":\"doc\"}");

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(Map.of("type", "doc", "content", List.of()));
    }

    @Test
    void should_preserve_structure_when_all_top_level_blocks_mixed() throws Exception {
        // given
        String json = doc(
                paragraph(text("안녕")),
                heading("2", text("소제목")),
                heading("3", text("작은 제목")),
                bulletList(listItem(paragraph(text("하나")))),
                orderedList(listItem(paragraph(text("첫째"))), listItem(paragraph(text("둘째")))),
                blockquote(paragraph(text("인용"))),
                concertCard());
        Map<String, Object> content = parse(json);

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(parse(json));
    }

    @Test
    void should_keep_bold_strike_and_http_links_when_allowed_marks_given() throws Exception {
        // given
        String json = doc(paragraph(
                markedText("굵게", mark("bold"), mark("strike")),
                markedText("링크1", link("\"http://example.com\"")),
                markedText("링크2", link("\"https://example.com\"")),
                markedText("링크3", link("\"HTTPS://EXAMPLE.COM\""))));
        Map<String, Object> content = parse(json);

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(parse(json));
    }

    @Test
    void should_keep_hard_break_when_inside_paragraph() throws Exception {
        // given
        String json = doc(paragraph(text("첫 줄"), hardBreak(), text("둘째 줄")));
        Map<String, Object> content = parse(json);

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(parse(json));
    }

    @Test
    void should_keep_start_attr_when_ordered_list_has_start() throws Exception {
        // given
        String json = doc("{\"type\":\"orderedList\",\"attrs\":{\"start\":3},\"content\":["
                + listItem(paragraph(text("셋째"))) + "]}");
        Map<String, Object> content = parse(json);

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(parse(json));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_omit_attrs_when_ordered_list_has_no_start() throws Exception {
        // given
        Map<String, Object> content = parse(doc(orderedList(listItem(paragraph(text("첫째"))))));

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        Map<String, Object> list = (Map<String, Object>) ((List<?>) result.get("content")).getFirst();
        assertThat(list).doesNotContainKey("attrs");
    }

    @Test
    void should_keep_all_six_attrs_when_track_card_with_release_group_id_given() throws Exception {
        // given
        String json = doc(card("\"entityType\":\"TRACK\",\"entityId\":9999999999,\"title\":\"Love wins all\","
                + "\"subtitle\":\"IU\",\"thumbnailUrl\":\"https://img/1.jpg\",\"releaseGroupId\":42"));
        Map<String, Object> content = parse(json);

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(parse(json));
    }

    @Test
    void should_pass_when_card_subtitle_and_thumbnail_url_are_null() throws Exception {
        // given
        String json = doc(card("\"entityType\":\"ARTIST\",\"entityId\":1,\"title\":\"IU\","
                + "\"subtitle\":null,\"thumbnailUrl\":null"));
        Map<String, Object> content = parse(json);

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(parse(json));
    }

    // ===== 제거 =====

    @Test
    void should_remove_disallowed_attrs_when_extra_attr_keys_given() throws Exception {
        // given
        Map<String, Object> content = parse(doc(
                "{\"type\":\"orderedList\",\"attrs\":{\"start\":2,\"type\":\"a\"},\"content\":["
                        + listItem(paragraph(text("둘째"))) + "]}",
                "{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"center\"},\"content\":[" + text("가운데") + "]}",
                "{\"type\":\"heading\",\"attrs\":{\"level\":2,\"id\":\"h-1\"},\"content\":[" + text("제목") + "]}",
                paragraph(markedText("링크", "{\"type\":\"link\",\"attrs\":{\"href\":\"https://example.com\","
                        + "\"target\":\"_blank\",\"rel\":\"noopener\",\"class\":\"x\"}}")),
                card("\"entityType\":\"CONCERT\",\"entityId\":7,\"title\":\"IU 콘서트\",\"extra\":\"x\"")));

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(parse(doc(
                "{\"type\":\"orderedList\",\"attrs\":{\"start\":2},\"content\":["
                        + listItem(paragraph(text("둘째"))) + "]}",
                paragraph(text("가운데")),
                heading("2", text("제목")),
                paragraph(markedText("링크", link("\"https://example.com\""))),
                concertCard())));
    }

    @Test
    void should_remove_marks_when_hard_break_has_marks() throws Exception {
        // given
        Map<String, Object> content = parse(doc(paragraph(
                text("a"), "{\"type\":\"hardBreak\",\"marks\":[{\"type\":\"bold\"}]}", text("b"))));

        // when
        Map<String, Object> result = TiptapContentValidator.validate(content);

        // then
        assertThat(result).isEqualTo(parse(doc(paragraph(text("a"), hardBreak(), text("b")))));
    }

    @Test
    void should_not_modify_original_when_disallowed_attrs_removed() throws Exception {
        // given
        String json = doc(
                "{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"center\"},\"content\":[" + text("가운데") + "]}",
                card("\"entityType\":\"CONCERT\",\"entityId\":7,\"title\":\"IU 콘서트\",\"extra\":\"x\""));
        Map<String, Object> content = parse(json);

        // when
        TiptapContentValidator.validate(content);

        // then
        assertThat(content).isEqualTo(parse(json));
    }

    // ===== 거부 =====

    @Test
    void should_throw_when_content_is_null() {
        // given
        Object content = null;

        // when & then
        assertThatThrownBy(() -> TiptapContentValidator.validate(content))
                .isInstanceOf(ContentInvalidFormatException.class);
    }

    @Test
    void should_throw_when_content_is_not_map() {
        // given
        Object content = List.of(Map.of("type", "doc"));

        // when & then
        assertThatThrownBy(() -> TiptapContentValidator.validate(content))
                .isInstanceOf(ContentInvalidFormatException.class);
    }

    static Stream<Arguments> invalidDocuments() {
        return Stream.of(
                // 루트
                Arguments.of("root is paragraph", paragraph(text("a"))),
                Arguments.of("doc content is not list", "{\"type\":\"doc\",\"content\":\"a\"}"),
                // 허용 밖 노드
                Arguments.of("codeBlock", doc("{\"type\":\"codeBlock\",\"content\":[" + text("x") + "]}")),
                Arguments.of("horizontalRule", doc("{\"type\":\"horizontalRule\"}")),
                Arguments.of("entityMentionChip", doc(paragraph(
                        "{\"type\":\"entityMentionChip\",\"attrs\":{\"entityType\":\"CONCERT\",\"entityId\":7,\"title\":\"t\"}}"))),
                Arguments.of("image", doc("{\"type\":\"image\",\"attrs\":{\"src\":\"https://img/1.jpg\"}}")),
                Arguments.of("node without type", doc("{\"content\":[]}")),
                // 허용 밖 마크
                Arguments.of("italic", doc(paragraph(markedText("a", mark("italic"))))),
                Arguments.of("underline", doc(paragraph(markedText("a", mark("underline"))))),
                Arguments.of("code", doc(paragraph(markedText("a", mark("code"))))),
                Arguments.of("textStyle", doc(paragraph(markedText("a",
                        "{\"type\":\"textStyle\",\"attrs\":{\"color\":\"red\"}}")))),
                // heading level
                Arguments.of("heading level 1", doc(heading("1", text("a")))),
                Arguments.of("heading level 4", doc(heading("4", text("a")))),
                Arguments.of("heading without level", doc("{\"type\":\"heading\",\"content\":[" + text("a") + "]}")),
                Arguments.of("heading level string", doc(heading("\"2\"", text("a")))),
                // link href
                Arguments.of("javascript href", doc(paragraph(markedText("a", link("\"javascript:alert(1)\""))))),
                Arguments.of("mailto href", doc(paragraph(markedText("a", link("\"mailto:a@b.com\""))))),
                Arguments.of("relative href", doc(paragraph(markedText("a", link("\"/posts/1\""))))),
                Arguments.of("link without href", doc(paragraph(markedText("a", "{\"type\":\"link\",\"attrs\":{}}")))),
                // 중첩
                Arguments.of("nested list", doc(bulletList(listItem(paragraph(text("a")),
                        bulletList(listItem(paragraph(text("b")))))))),
                Arguments.of("heading in listItem", doc(bulletList(listItem(heading("2", text("a")))))),
                Arguments.of("heading in blockquote", doc(blockquote(heading("2", text("a"))))),
                Arguments.of("bulletList in blockquote", doc(blockquote(bulletList(listItem(paragraph(text("a"))))))),
                Arguments.of("card in blockquote", doc(blockquote(concertCard()))),
                Arguments.of("paragraph in paragraph", doc(paragraph(paragraph(text("a"))))),
                Arguments.of("top level text", doc(text("a"))),
                Arguments.of("top level listItem", doc(listItem(paragraph(text("a"))))),
                Arguments.of("top level hardBreak", doc(hardBreak())),
                Arguments.of("paragraph in bulletList", doc(bulletList(paragraph(text("a"))))),
                // 최소 개수
                Arguments.of("empty bulletList", doc(bulletList())),
                Arguments.of("empty orderedList", doc(orderedList())),
                Arguments.of("empty listItem", doc(bulletList(listItem()))),
                Arguments.of("empty blockquote", doc(blockquote())),
                Arguments.of("blockquote without content", doc("{\"type\":\"blockquote\"}")),
                // orderedList start
                Arguments.of("start string", doc("{\"type\":\"orderedList\",\"attrs\":{\"start\":\"2\"},\"content\":["
                        + listItem(paragraph(text("a"))) + "]}")),
                Arguments.of("start decimal", doc("{\"type\":\"orderedList\",\"attrs\":{\"start\":1.5},\"content\":["
                        + listItem(paragraph(text("a"))) + "]}")),
                // card
                Arguments.of("card entityType USER", doc(card("\"entityType\":\"USER\",\"entityId\":1,\"title\":\"t\""))),
                Arguments.of("card without entityId", doc(card("\"entityType\":\"ARTIST\",\"title\":\"t\""))),
                Arguments.of("card entityId string", doc(card("\"entityType\":\"ARTIST\",\"entityId\":\"1\",\"title\":\"t\""))),
                Arguments.of("card entityId decimal", doc(card("\"entityType\":\"ARTIST\",\"entityId\":1.5,\"title\":\"t\""))),
                Arguments.of("card without title", doc(card("\"entityType\":\"ARTIST\",\"entityId\":1"))),
                Arguments.of("card subtitle number", doc(card("\"entityType\":\"ARTIST\",\"entityId\":1,\"title\":\"t\",\"subtitle\":3"))),
                Arguments.of("releaseGroupId on CONCERT card", doc(card(
                        "\"entityType\":\"CONCERT\",\"entityId\":7,\"title\":\"t\",\"releaseGroupId\":42"))),
                Arguments.of("card with content child", doc("{\"type\":\"entityMentionCard\",\"attrs\":{"
                        + "\"entityType\":\"CONCERT\",\"entityId\":7,\"title\":\"t\"},\"content\":[" + text("a") + "]}")),
                // text
                Arguments.of("text is not string", doc(paragraph("{\"type\":\"text\",\"text\":1}"))),
                Arguments.of("marks is not list", doc(paragraph("{\"type\":\"text\",\"text\":\"a\",\"marks\":{\"type\":\"bold\"}}"))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDocuments")
    void should_throw_when_invalid_document_given(String description, String json) throws Exception {
        // given
        Map<String, Object> content = parse(json);

        // when & then
        assertThatThrownBy(() -> TiptapContentValidator.validate(content))
                .isInstanceOf(ContentInvalidFormatException.class);
    }
}
