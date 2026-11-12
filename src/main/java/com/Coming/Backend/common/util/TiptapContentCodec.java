package com.Coming.Backend.common.util;

import com.Coming.Backend.common.exception.ContentTooLongException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 게시글·공지사항 본문(Tiptap JSON)의 저장 전 처리와 저장값 읽기를 맡는다.
 * 저장 전에는 공통 스키마 검증(허용 밖 attrs 제거) → 텍스트 길이 검사 → 직렬화 → JSON 길이 검사 순서로 처리한다.
 * Spring이 HTTP 메시지 변환에 쓰는 Jackson 인스턴스(3.x)와 무관하게 항상 Jackson 2 databind로 직렬화·역직렬화한다.
 */
public final class TiptapContentCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int MAX_TEXT_LENGTH = 10000;

    /**
     * 직렬화한 JSON 크기 상한. 텍스트는 텍스트 노드만 추출한 값이라
     * 구조만 방대한 JSON으로 MAX_TEXT_LENGTH 검증을 우회할 수 있어 별도로 제한한다.
     */
    private static final int MAX_JSON_LENGTH = 50000;

    private TiptapContentCodec() {
    }

    /**
     * 검증·정리를 마친 본문. tree는 정리된 문서, json은 저장할 직렬화 문자열, text는 검색·길이 검사용 텍스트다.
     */
    public record SanitizedContent(Map<String, Object> tree, String json, String text) {
    }

    /**
     * 본문을 검증·정리하고 길이를 검사한다. 형식 위반은 ContentInvalidFormatException,
     * 텍스트 10,000자 또는 JSON 50,000자 초과는 ContentTooLongException을 던진다.
     */
    public static SanitizedContent sanitize(Object content) {
        Map<String, Object> tree = TiptapContentValidator.validate(content);
        String text = TiptapTextExtractor.extract(tree);
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new ContentTooLongException();
        }
        String json = write(tree);
        if (json.length() > MAX_JSON_LENGTH) {
            throw new ContentTooLongException();
        }
        return new SanitizedContent(tree, json, text);
    }

    /**
     * 저장된 본문 JSON 문자열을 Map/List 트리로 읽는다.
     */
    public static Object read(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 본문 content가 유효한 JSON이 아닙니다.", e);
        }
    }

    private static String write(Object content) {
        try {
            return OBJECT_MAPPER.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("본문 content 직렬화에 실패했습니다.", e);
        }
    }
}
