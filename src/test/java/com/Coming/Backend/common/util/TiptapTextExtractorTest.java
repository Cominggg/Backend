package com.Coming.Backend.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TiptapTextExtractorTest {

    @Test
    void should_extract_single_text_node() {
        // given
        Map<String, Object> content = Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "안녕하세요")
                        ))
                )
        );

        // when
        String result = TiptapTextExtractor.extract(content);

        // then
        assertThat(result).isEqualTo("안녕하세요");
    }

    @Test
    void should_concatenate_adjacent_text_nodes_without_space_within_same_paragraph() {
        // given: 굵게 등 서식으로 나뉜 인접 텍스트 노드("안녕"+"하세요")는 원문 그대로 붙여야 한다.
        Map<String, Object> content = Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "안녕"),
                                Map.of("type", "text", "text", "하세요")
                        ))
                )
        );

        // when
        String result = TiptapTextExtractor.extract(content);

        // then
        assertThat(result).isEqualTo("안녕하세요");
    }

    @Test
    void should_join_separate_paragraphs_with_space() {
        // given
        Map<String, Object> content = Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "첫 문단")
                        )),
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "둘째 문단")
                        ))
                )
        );

        // when
        String result = TiptapTextExtractor.extract(content);

        // then
        assertThat(result).isEqualTo("첫 문단 둘째 문단");
    }

    @Test
    void should_ignore_non_text_nodes_such_as_mention() {
        // given: 실제 작성 내용에 포함된 공백은 텍스트 노드 자체에 들어있으므로 그대로 보존한다.
        Map<String, Object> content = Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "공연 "),
                                Map.of("type", "mention", "attrs", Map.of("entityType", "CONCERT", "entityId", 1)),
                                Map.of("type", "text", "text", "다녀왔어요")
                        ))
                )
        );

        // when
        String result = TiptapTextExtractor.extract(content);

        // then
        assertThat(result).isEqualTo("공연 다녀왔어요");
    }

    @Test
    void should_return_empty_string_when_no_text_node_exists() {
        // given
        Map<String, Object> content = Map.of("type", "doc", "content", List.of());

        // when
        String result = TiptapTextExtractor.extract(content);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_string_when_content_is_null() {
        // when
        String result = TiptapTextExtractor.extract(null);

        // then
        assertThat(result).isEmpty();
    }

    // 웹·앱과 공유한 글자 수 기준값(Specification spec/client/post-content.md)을 고정한다.

    static Stream<Arguments> structureCases() {
        return Stream.of(
                Arguments.of("N1", doc(p(text("a"))), "a"),
                Arguments.of("N2", doc(p(text("a")), p(text("b"))), "a b"),
                Arguments.of("N3", doc(blockquote(p(text("a"))), p(text("b"))), "a  b"),
                Arguments.of("N4", doc(bulletList(listItem(p(text("a")))), p(text("b"))), "a   b"),
                Arguments.of("N5", doc(bulletList(listItem(p(text("a"))), listItem(p(text("b"))))), "a  b"),
                Arguments.of("N6", doc(orderedList(listItem(p(text("a"))), listItem(p(text("b")))), p(text("c"))),
                        "a  b   c"),
                Arguments.of("N7", doc(heading(2, text("ab")), p(text("c"))), "ab c"),
                Arguments.of("N8", doc(p(text("a"), hardBreak(), text("b"))), "ab"),
                Arguments.of("N9", doc(p(text("a")), card(), p(text("b"))), "a b"),
                Arguments.of("N10", doc(card()), ""),
                Arguments.of("N11", doc(p(text("a")), Map.of("type", "paragraph"), p(text("b"))), "a b"),
                Arguments.of("N12", doc(p(text("ab"), boldText("c"))), "abc"),
                Arguments.of("N13", doc(p(text("  a  b  "))), "a  b"),
                Arguments.of("N14", doc(p(text("😀"))), "😀")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("structureCases")
    void should_extract_expected_text_given_shared_structure_case(String caseName, Map<String, Object> content,
                                                                   String expected) {
        // given: caseName, content

        // when
        String result = TiptapTextExtractor.extract(content);

        // then
        assertThat(result).isEqualTo(expected);
        assertThat(result.length()).isEqualTo(expected.length());
    }

    static Stream<Arguments> boundaryCases() {
        return Stream.of(
                Arguments.of("B1", doc(p(text("가".repeat(9999)))), 9999),
                Arguments.of("B2", doc(p(text("가".repeat(10000)))), 10000),
                Arguments.of("B3", doc(p(text("가".repeat(10001)))), 10001),
                Arguments.of("B4", doc(blockquote(p(text("가".repeat(9997)))), p(text("나"))), 10000),
                Arguments.of("B5", doc(blockquote(p(text("가".repeat(9998)))), p(text("나"))), 10001),
                Arguments.of("B6", doc(bulletList(listItem(p(text("가".repeat(9996))))), p(text("나"))), 10000),
                Arguments.of("B7", doc(bulletList(listItem(p(text("가".repeat(9997))))), p(text("나"))), 10001)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("boundaryCases")
    void should_return_expected_length_given_shared_boundary_case(String caseName, Map<String, Object> content,
                                                                  int expectedLength) {
        // given: caseName, content

        // when
        String result = TiptapTextExtractor.extract(content);

        // then
        assertThat(result.length()).isEqualTo(expectedLength);
    }

    private static Map<String, Object> doc(Object... children) {
        return Map.of("type", "doc", "content", List.of(children));
    }

    private static Map<String, Object> p(Object... children) {
        return Map.of("type", "paragraph", "content", List.of(children));
    }

    private static Map<String, Object> text(String value) {
        return Map.of("type", "text", "text", value);
    }

    private static Map<String, Object> boldText(String value) {
        return Map.of("type", "text", "text", value, "marks", List.of(Map.of("type", "bold")));
    }

    private static Map<String, Object> blockquote(Object... children) {
        return Map.of("type", "blockquote", "content", List.of(children));
    }

    private static Map<String, Object> bulletList(Object... items) {
        return Map.of("type", "bulletList", "content", List.of(items));
    }

    private static Map<String, Object> orderedList(Object... items) {
        return Map.of("type", "orderedList", "content", List.of(items));
    }

    private static Map<String, Object> listItem(Object... children) {
        return Map.of("type", "listItem", "content", List.of(children));
    }

    private static Map<String, Object> heading(int level, Object... children) {
        return Map.of("type", "heading", "attrs", Map.of("level", level), "content", List.of(children));
    }

    private static Map<String, Object> hardBreak() {
        return Map.of("type", "hardBreak");
    }

    private static Map<String, Object> card() {
        return Map.of("type", "entityMentionCard",
                "attrs", Map.of("entityType", "CONCERT", "entityId", 1, "title", "공연"));
    }
}
