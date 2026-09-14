package com.Coming.Backend.post.util;

import java.util.List;
import java.util.Map;

public final class TiptapTextExtractor {

    private TiptapTextExtractor() {
    }

    /**
     * Tiptap 문서(JSON을 역직렬화한 Map/List 트리)에서 텍스트 노드만 추출해 검색용 텍스트를 반환한다.
     * 같은 문단 안에서 서식(굵게 등)으로 나뉜 인접 텍스트 노드는 원문 그대로 붙여 쓰고,
     * 문단 등 블록 경계에서만 공백을 넣는다. 멘션 등 텍스트 노드가 아닌 노드는 무시한다.
     */
    public static String extract(Object content) {
        StringBuilder builder = new StringBuilder();
        collect(content, builder);
        return builder.toString().trim();
    }

    private static void collect(Object node, StringBuilder builder) {
        if (node instanceof List<?> list) {
            list.forEach(child -> collect(child, builder));
            return;
        }
        if (!(node instanceof Map<?, ?> map)) {
            return;
        }
        if (isTextNode(map)) {
            if (map.get("text") instanceof String textValue) {
                builder.append(textValue);
            }
            return;
        }
        int lengthBeforeBlock = builder.length();
        collect(map.get("content"), builder);
        if (builder.length() > lengthBeforeBlock) {
            builder.append(' ');
        }
    }

    private static boolean isTextNode(Map<?, ?> node) {
        return "text".equals(node.get("type"));
    }
}
