package com.Coming.Backend.post.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TiptapConcertMentionFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> parse(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    private static String card(String entityType, String entityIdJson) {
        return "{\"type\":\"entityMentionCard\",\"attrs\":{\"entityType\":\"" + entityType
                + "\",\"entityId\":" + entityIdJson + ",\"title\":\"제목\",\"thumbnailUrl\":\"https://img/1.jpg\"}}";
    }

    private static String chip(String entityType, String entityIdJson) {
        return "{\"type\":\"entityMentionChip\",\"attrs\":{\"entityType\":\"" + entityType
                + "\",\"entityId\":" + entityIdJson + ",\"title\":\"제목\"}}";
    }

    private static String paragraph(String... inlineNodes) {
        return "{\"type\":\"paragraph\",\"content\":[" + String.join(",", inlineNodes) + "]}";
    }

    private static String text(String value) {
        return "{\"type\":\"text\",\"text\":\"" + value + "\"}";
    }

    private static String doc(String... blocks) {
        return "{\"type\":\"doc\",\"content\":[" + String.join(",", blocks) + "]}";
    }

    // ===== collectConcertIds =====

    @Test
    void should_collect_concert_ids_when_top_level_card_and_inline_chip_given() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("CONCERT", "7"), paragraph(text("보러 감 "), chip("CONCERT", "12"))));

        // when
        Set<Long> result = TiptapConcertMentionFilter.collectConcertIds(content);

        // then
        assertThat(result).containsExactlyInAnyOrder(7L, 12L);
    }

    @Test
    void should_exclude_artist_mention_when_collecting_concert_ids() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("ARTIST", "3"), paragraph(chip("ARTIST", "4"), chip("CONCERT", "7"))));

        // when
        Set<Long> result = TiptapConcertMentionFilter.collectConcertIds(content);

        // then
        assertThat(result).containsExactly(7L);
    }

    @Test
    void should_skip_concert_mention_when_entity_id_is_string() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("CONCERT", "\"7\""), chip("CONCERT", "8")));

        // when
        Set<Long> result = TiptapConcertMentionFilter.collectConcertIds(content);

        // then
        assertThat(result).containsExactly(8L);
    }

    @Test
    void should_skip_concert_mention_when_entity_id_missing() throws Exception {
        // given
        Map<String, Object> content = parse(doc(
                "{\"type\":\"entityMentionCard\",\"attrs\":{\"entityType\":\"CONCERT\",\"title\":\"제목\"}}",
                card("CONCERT", "8")));

        // when
        Set<Long> result = TiptapConcertMentionFilter.collectConcertIds(content);

        // then
        assertThat(result).containsExactly(8L);
    }

    @Test
    void should_collect_as_long_when_entity_id_is_integer_or_long() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("CONCERT", "7"), card("CONCERT", "9999999999")));

        // when
        Set<Long> result = TiptapConcertMentionFilter.collectConcertIds(content);

        // then
        assertThat(result).containsExactlyInAnyOrder(7L, 9_999_999_999L);
    }

    @Test
    void should_return_empty_set_when_content_is_null() {
        // given
        Object content = null;

        // when
        Set<Long> result = TiptapConcertMentionFilter.collectConcertIds(content);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_set_when_document_is_empty() throws Exception {
        // given
        Map<String, Object> content = parse(doc());

        // when
        Set<Long> result = TiptapConcertMentionFilter.collectConcertIds(content);

        // then
        assertThat(result).isEmpty();
    }

    // ===== removeConcertMentionsExcept =====

    @Test
    void should_keep_visible_card_and_remove_hidden_card_when_filtering() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("CONCERT", "7"), card("CONCERT", "8")));

        // when
        Object result = TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of(7L));

        // then
        assertThat(result).isEqualTo(parse(doc(card("CONCERT", "7"))));
    }

    @Test
    void should_remove_hidden_chip_and_keep_surrounding_text_when_filtering() throws Exception {
        // given
        Map<String, Object> content = parse(doc(paragraph(text("앞 "), chip("CONCERT", "8"), text(" 뒤"))));

        // when
        Object result = TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of());

        // then
        assertThat(result).isEqualTo(parse(doc(paragraph(text("앞 "), text(" 뒤")))));
    }

    @Test
    void should_keep_artist_mention_when_filtering() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("ARTIST", "3"), paragraph(chip("ARTIST", "4"))));

        // when
        Object result = TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of());

        // then
        assertThat(result).isEqualTo(content);
    }

    @Test
    void should_return_doc_with_empty_content_when_only_hidden_card_existed() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("CONCERT", "8")));

        // when
        Object result = TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of());

        // then
        assertThat(result).isEqualTo(Map.of("type", "doc", "content", List.of()));
        assertThat(objectMapper.writeValueAsString(result)).isEqualTo("{\"type\":\"doc\",\"content\":[]}");
    }

    @Test
    void should_not_modify_original_when_filtering() throws Exception {
        // given
        String json = doc(card("CONCERT", "8"), paragraph(text("앞 "), chip("CONCERT", "9")));
        Map<String, Object> content = parse(json);

        // when
        TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of());

        // then
        assertThat(content).isEqualTo(parse(json));
    }

    @Test
    void should_remove_concert_mention_when_entity_id_is_not_number() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("CONCERT", "\"7\""), paragraph(chip("CONCERT", "null"))));

        // when
        Object result = TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of(7L));

        // then
        assertThat(result).isEqualTo(parse(doc(paragraph())));
    }

    @Test
    void should_remove_concert_mention_when_entity_id_is_fractional() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("CONCERT", "7.5"), card("CONCERT", "7")));

        // when
        Object result = TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of(7L));

        // then
        assertThat(result).isEqualTo(parse(doc(card("CONCERT", "7"))));
    }

    @Test
    void should_keep_node_without_type_when_filtering() throws Exception {
        // given
        Map<String, Object> content = parse(doc("{\"content\":[" + card("CONCERT", "7") + "]}"));

        // when
        Object result = TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of());

        // then
        assertThat(result).isEqualTo(parse(doc("{\"content\":[]}")));
    }

    @Test
    void should_keep_node_when_content_key_missing() throws Exception {
        // given
        Map<String, Object> content = parse("{\"type\":\"doc\"}");

        // when
        Object result = TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of());

        // then
        assertThat(result).isEqualTo(new HashMap<>(Map.of("type", "doc")));
    }

    @Test
    void should_return_null_when_content_is_null() {
        // given
        Object content = null;

        // when
        Object result = TiptapConcertMentionFilter.removeConcertMentionsExcept(content, Set.of());

        // then
        assertThat(result).isNull();
    }
}
