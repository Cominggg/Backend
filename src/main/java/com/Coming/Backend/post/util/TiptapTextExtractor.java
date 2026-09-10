package com.Coming.Backend.post.util;

import java.util.List;
import java.util.Map;

public final class TiptapTextExtractor {

    private TiptapTextExtractor() {
    }

    /**
     * Tiptap 문서(JSON을 역직렬화한 Map/List 트리)에서 텍스트 노드만 추출해 공백으로 이어붙인
     * 검색용 텍스트를 반환한다. 멘션 등 텍스트 노드가 아닌 노드는 무시한다.
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
        Object text = map.get("text");
        if (isTextNode(map) && text instanceof String textValue) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(textValue);
        }
        collect(map.get("content"), builder);
    }

    private static boolean isTextNode(Map<?, ?> node) {
        return "text".equals(node.get("type"));
    }
}
