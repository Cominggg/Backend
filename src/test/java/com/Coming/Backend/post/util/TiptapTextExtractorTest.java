package com.Coming.Backend.post.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

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
}
