package com.Coming.Backend.common.util;

import com.Coming.Backend.common.exception.ContentInvalidFormatException;
import com.Coming.Backend.post.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 게시글 본문(Tiptap JSON을 역직렬화한 Map/List 트리)을 웹·앱 공통 스키마(Specification spec/client/post-content.md)로 검증한다.
 * 허용 밖 노드·마크, 위치·중첩 규칙 위반, 허용 밖 heading level, http·https 외 링크는 ContentInvalidFormatException을 던진다.
 * 허용 밖 attrs 키와 노드의 그 밖의 키(예: hardBreak의 marks)는 오류 없이 지운 사본을 반환한다. 원본은 바꾸지 않는다.
 */
public final class TiptapContentValidator {

    private static final Set<String> TOP_LEVEL_TYPES =
            Set.of("paragraph", "heading", "bulletList", "orderedList", "blockquote", "entityMentionCard");
    private static final Set<String> INLINE_TYPES = Set.of("text", "hardBreak");
    private static final Set<String> PARAGRAPH_ONLY = Set.of("paragraph");
    private static final Set<String> LIST_ITEM_ONLY = Set.of("listItem");
    private static final Set<Integer> HEADING_LEVELS = Set.of(2, 3);
    private static final Set<String> SIMPLE_MARK_TYPES = Set.of("bold", "strike");
    private static final Set<String> ENTITY_TYPES =
            Arrays.stream(EntityType.values()).map(Enum::name).collect(Collectors.toUnmodifiableSet());
    private static final List<String> CARD_ATTR_KEYS =
            List.of("entityType", "entityId", "title", "subtitle", "thumbnailUrl", "releaseGroupId");

    private TiptapContentValidator() {
    }

    /**
     * 본문을 검증하고 허용 밖 attrs를 지운 사본을 반환한다.
     */
    public static Map<String, Object> validate(Object content) {
        Map<?, ?> doc = asNode(content);
        if (!"doc".equals(doc.get("type"))) {
            throw new ContentInvalidFormatException();
        }
        Map<String, Object> result = node("doc");
        result.put("content", children(doc, TOP_LEVEL_TYPES, 0));
        return result;
    }

    private static Map<String, Object> sanitize(Map<?, ?> node) {
        String type = (String) node.get("type");
        return switch (type) {
            case "paragraph" -> withContent(node("paragraph"), node, INLINE_TYPES, 0);
            case "heading" -> withContent(heading(node), node, INLINE_TYPES, 0);
            case "bulletList" -> withContent(node("bulletList"), node, LIST_ITEM_ONLY, 1);
            case "orderedList" -> withContent(orderedList(node), node, LIST_ITEM_ONLY, 1);
            case "listItem", "blockquote" -> withContent(node(type), node, PARAGRAPH_ONLY, 1);
            case "entityMentionCard" -> entityMentionCard(node);
            case "text" -> text(node);
            case "hardBreak" -> {
                requireNoContent(node);
                yield node("hardBreak");
            }
            default -> throw new ContentInvalidFormatException();
        };
    }

    /**
     * content의 자식이 모두 allowedTypes이고 minCount개 이상인지 확인하고, 각 자식을 정리한 목록을 반환한다.
     * content 키가 없으면 빈 목록으로 본다.
     */
    private static List<Object> children(Map<?, ?> node, Set<String> allowedTypes, int minCount) {
        Object content = node.get("content");
        List<?> rawChildren = switch (content) {
            case null -> List.of();
            case List<?> list -> list;
            default -> throw new ContentInvalidFormatException();
        };
        if (rawChildren.size() < minCount) {
            throw new ContentInvalidFormatException();
        }
        List<Object> result = new ArrayList<>(rawChildren.size());
        for (Object rawChild : rawChildren) {
            Map<?, ?> child = asNode(rawChild);
            if (!allowedTypes.contains((String) child.get("type"))) {
                throw new ContentInvalidFormatException();
            }
            result.add(sanitize(child));
        }
        return result;
    }

    private static Map<String, Object> withContent(Map<String, Object> result, Map<?, ?> source,
                                                   Set<String> allowedTypes, int minCount) {
        List<Object> content = children(source, allowedTypes, minCount);
        if (source.containsKey("content")) {
            result.put("content", content);
        }
        return result;
    }

    private static Map<String, Object> heading(Map<?, ?> node) {
        Object level = attrs(node).get("level");
        if (!(level instanceof Integer intLevel) || !HEADING_LEVELS.contains(intLevel)) {
            throw new ContentInvalidFormatException();
        }
        return node("heading", Map.of("level", level));
    }

    private static Map<String, Object> orderedList(Map<?, ?> node) {
        Object start = attrs(node).get("start");
        if (start == null) {
            return node("orderedList");
        }
        if (!isInteger(start)) {
            throw new ContentInvalidFormatException();
        }
        return node("orderedList", Map.of("start", start));
    }

    private static Map<String, Object> entityMentionCard(Map<?, ?> node) {
        requireNoContent(node);
        Map<?, ?> attrs = attrs(node);
        Object entityType = attrs.get("entityType");
        Object releaseGroupId = attrs.get("releaseGroupId");
        boolean isValid = entityType instanceof String type && ENTITY_TYPES.contains(type)
                && isInteger(attrs.get("entityId"))
                && attrs.get("title") instanceof String
                && isOptionalString(attrs.get("subtitle"))
                && isOptionalString(attrs.get("thumbnailUrl"))
                && (releaseGroupId == null || (isInteger(releaseGroupId) && EntityType.TRACK.name().equals(entityType)));
        if (!isValid) {
            throw new ContentInvalidFormatException();
        }
        Map<String, Object> sanitizedAttrs = new LinkedHashMap<>();
        for (String key : CARD_ATTR_KEYS) {
            if (attrs.containsKey(key)) {
                sanitizedAttrs.put(key, attrs.get(key));
            }
        }
        return node("entityMentionCard", sanitizedAttrs);
    }

    private static Map<String, Object> text(Map<?, ?> node) {
        if (!(node.get("text") instanceof String text)) {
            throw new ContentInvalidFormatException();
        }
        Map<String, Object> result = node("text");
        result.put("text", text);
        Object marks = node.get("marks");
        if (marks != null) {
            if (!(marks instanceof List<?> rawMarks)) {
                throw new ContentInvalidFormatException();
            }
            result.put("marks", rawMarks.stream().map(TiptapContentValidator::mark).toList());
        }
        return result;
    }

    private static Map<String, Object> mark(Object rawMark) {
        Map<?, ?> mark = asNode(rawMark);
        Object type = mark.get("type");
        if (SIMPLE_MARK_TYPES.contains((String) type)) {
            return node((String) type);
        }
        if ("link".equals(type) && attrs(mark).get("href") instanceof String href && isHttpUrl(href)) {
            return node("link", Map.of("href", href));
        }
        throw new ContentInvalidFormatException();
    }

    /**
     * atom·leaf 노드(entityMentionCard, hardBreak)는 자식을 가질 수 없다. 빈 content는 허용한다.
     */
    private static void requireNoContent(Map<?, ?> node) {
        if (node.get("content") instanceof List<?> content && !content.isEmpty()) {
            throw new ContentInvalidFormatException();
        }
    }

    /**
     * type이 문자열인 Map 노드만 받는다. type은 Set.of의 contains(null) NPE를 피하려고 여기서 먼저 확인한다.
     */
    private static Map<?, ?> asNode(Object value) {
        if (value instanceof Map<?, ?> map && map.get("type") instanceof String) {
            return map;
        }
        throw new ContentInvalidFormatException();
    }

    private static Map<?, ?> attrs(Map<?, ?> node) {
        return switch (node.get("attrs")) {
            case null -> Map.of();
            case Map<?, ?> attrs -> attrs;
            default -> throw new ContentInvalidFormatException();
        };
    }

    private static Map<String, Object> node(String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        return result;
    }

    private static Map<String, Object> node(String type, Map<String, Object> attrs) {
        Map<String, Object> result = node(type);
        result.put("attrs", attrs);
        return result;
    }

    private static boolean isInteger(Object value) {
        return value instanceof Integer || value instanceof Long;
    }

    private static boolean isOptionalString(Object value) {
        return value == null || value instanceof String;
    }

    private static boolean isHttpUrl(String href) {
        return href.regionMatches(true, 0, "http://", 0, 7) || href.regionMatches(true, 0, "https://", 0, 8);
    }
}
