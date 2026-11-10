package com.Coming.Backend.post.util;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.exception.PostContentInvalidFormatException;
import com.Coming.Backend.post.util.TiptapEntityTagExtractor.EntityTag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TiptapEntityTagExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> parse(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    private static String card(String entityType, String entityIdJson) {
        return "{\"type\":\"entityMentionCard\",\"attrs\":{\"entityType\":\"" + entityType
                + "\",\"entityId\":" + entityIdJson + ",\"title\":\"제목\",\"thumbnailUrl\":\"https://img/1.jpg\"}}";
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

    private static String[] concertCards(int startId, int count) {
        return IntStream.range(startId, startId + count)
                .mapToObj(id -> card("CONCERT", String.valueOf(id)))
                .toArray(String[]::new);
    }

    @Test
    void should_return_empty_list_when_no_card_given() throws Exception {
        // given
        Map<String, Object> content = parse(doc(paragraph(text("오늘 공연 최고였다"))));

        // when
        List<EntityTag> result = TiptapEntityTagExtractor.extract(content);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_tags_in_appearance_order_when_multiple_cards_given() throws Exception {
        // given
        Map<String, Object> content = parse(doc(
                card("CONCERT", "30"),
                paragraph(text("중간 문단")),
                card("ARTIST", "5"),
                card("RELEASE", "12"),
                card("TRACK", "1")));

        // when
        List<EntityTag> result = TiptapEntityTagExtractor.extract(content);

        // then
        assertThat(result).containsExactly(
                new EntityTag(EntityType.CONCERT, 30L),
                new EntityTag(EntityType.ARTIST, 5L),
                new EntityTag(EntityType.RELEASE, 12L),
                new EntityTag(EntityType.TRACK, 1L));
    }

    @Test
    void should_keep_first_occurrence_only_when_duplicate_card_given() throws Exception {
        // given
        Map<String, Object> content = parse(doc(
                card("CONCERT", "30"),
                card("ARTIST", "5"),
                card("CONCERT", "30"),
                card("RELEASE", "12"),
                card("ARTIST", "5")));

        // when
        List<EntityTag> result = TiptapEntityTagExtractor.extract(content);

        // then
        assertThat(result).containsExactly(
                new EntityTag(EntityType.CONCERT, 30L),
                new EntityTag(EntityType.ARTIST, 5L),
                new EntityTag(EntityType.RELEASE, 12L));
    }

    @Test
    void should_treat_as_distinct_when_same_id_with_different_type_given() throws Exception {
        // given
        Map<String, Object> content = parse(doc(
                card("CONCERT", "7"),
                card("ARTIST", "7"),
                card("RELEASE", "7"),
                card("TRACK", "7")));

        // when
        List<EntityTag> result = TiptapEntityTagExtractor.extract(content);

        // then
        assertThat(result).containsExactly(
                new EntityTag(EntityType.CONCERT, 7L),
                new EntityTag(EntityType.ARTIST, 7L),
                new EntityTag(EntityType.RELEASE, 7L),
                new EntityTag(EntityType.TRACK, 7L));
    }

    @Test
    void should_convert_to_long_when_entity_id_is_integer_or_long() throws Exception {
        // given
        Map<String, Object> content = parse(doc(card("CONCERT", "7"), card("ARTIST", "3000000000")));
        List<?> blocks = (List<?>) content.get("content");
        assertThat(((Map<?, ?>) ((Map<?, ?>) blocks.get(0)).get("attrs")).get("entityId")).isInstanceOf(Integer.class);
        assertThat(((Map<?, ?>) ((Map<?, ?>) blocks.get(1)).get("attrs")).get("entityId")).isInstanceOf(Long.class);

        // when
        List<EntityTag> result = TiptapEntityTagExtractor.extract(content);

        // then
        assertThat(result).containsExactly(
                new EntityTag(EntityType.CONCERT, 7L),
                new EntityTag(EntityType.ARTIST, 3_000_000_000L));
    }

    @Test
    void should_return_ten_tags_when_exactly_ten_unique_cards_given() throws Exception {
        // given
        Map<String, Object> content = parse(doc(concertCards(1, 10)));

        // when
        List<EntityTag> result = TiptapEntityTagExtractor.extract(content);

        // then
        assertThat(result).hasSize(10);
    }

    @Test
    void should_throw_post_content_invalid_format_exception_when_eleven_unique_cards_given() throws Exception {
        // given
        Map<String, Object> content = parse(doc(concertCards(1, 11)));

        // when & then
        assertThatThrownBy(() -> TiptapEntityTagExtractor.extract(content))
                .isInstanceOf(PostContentInvalidFormatException.class)
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.POST_CONTENT_INVALID_FORMAT.getMessage());
    }

    @Test
    void should_return_ten_tags_when_fifteen_cards_with_ten_unique_given() throws Exception {
        // given
        String[] uniqueCards = concertCards(1, 10);
        String[] duplicateCards = concertCards(1, 5);
        String[] allCards = new String[15];
        System.arraycopy(uniqueCards, 0, allCards, 0, 10);
        System.arraycopy(duplicateCards, 0, allCards, 10, 5);
        Map<String, Object> content = parse(doc(allCards));

        // when
        List<EntityTag> result = TiptapEntityTagExtractor.extract(content);

        // then
        assertThat(result).hasSize(10);
    }

    @Test
    void should_return_empty_list_when_content_is_null() {
        // given
        Object content = null;

        // when
        List<EntityTag> result = TiptapEntityTagExtractor.extract(content);

        // then
        assertThat(result).isEmpty();
    }
}
