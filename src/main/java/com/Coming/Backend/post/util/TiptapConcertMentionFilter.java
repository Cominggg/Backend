package com.Coming.Backend.post.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tiptap 문서(JSON을 역직렬화한 Map/List 트리)의 공연 멘션 노드(entityMentionCard·entityMentionChip)를 다룬다.
 * 멘션 노드 attrs에는 작성 시점의 제목·포스터가 저장돼 있어, 공연이 숨겨져도 본문에 그대로 남는다.
 */
public final class TiptapConcertMentionFilter {

    private static final Set<String> MENTION_NODE_TYPES = Set.of("entityMentionCard", "entityMentionChip");
    private static final String CONCERT_ENTITY_TYPE = "CONCERT";

    private TiptapConcertMentionFilter() {
    }

    /**
     * 문서에 있는 공연 멘션 노드의 공연 id를 모은다. entityId가 숫자가 아닌 노드는 건너뛴다.
     */
    public static Set<Long> collectConcertIds(Object content) {
        Set<Long> concertIds = new HashSet<>();
        collect(content, concertIds);
        return concertIds;
    }

    /**
     * visibleConcertIds에 없는 공연을 가리키는 멘션 노드를 뺀 문서 사본을 반환한다. 원본은 바꾸지 않는다.
     * entityId가 숫자가 아닌 공연 멘션도 표시할 수 없으므로 뺀다.
     */
    public static Object removeConcertMentionsExcept(Object content, Set<Long> visibleConcertIds) {
        if (content instanceof List<?> list) {
            List<Object> kept = new ArrayList<>();
            for (Object child : list) {
                if (!isHiddenConcertMention(child, visibleConcertIds)) {
                    kept.add(removeConcertMentionsExcept(child, visibleConcertIds));
                }
            }
            return kept;
        }
        if (content instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>(map);
            if (map.containsKey("content")) {
                copy.put("content", removeConcertMentionsExcept(map.get("content"), visibleConcertIds));
            }
            return copy;
        }
        return content;
    }

    private static void collect(Object node, Set<Long> concertIds) {
        if (node instanceof List<?> list) {
            list.forEach(child -> collect(child, concertIds));
            return;
        }
        if (!(node instanceof Map<?, ?> map)) {
            return;
        }
        if (isConcertMention(map) && concertIdOf(map) instanceof Long concertId) {
            concertIds.add(concertId);
        }
        collect(map.get("content"), concertIds);
    }

    private static boolean isHiddenConcertMention(Object node, Set<Long> visibleConcertIds) {
        if (!(node instanceof Map<?, ?> map) || !isConcertMention(map)) {
            return false;
        }
        Long concertId = concertIdOf(map);
        return concertId == null || !visibleConcertIds.contains(concertId);
    }

    private static boolean isConcertMention(Map<?, ?> node) {
        // Set.of는 contains(null)에서 NPE를 던지므로 type이 문자열인지 먼저 본다
        return node.get("type") instanceof String type
                && MENTION_NODE_TYPES.contains(type)
                && node.get("attrs") instanceof Map<?, ?> attrs
                && CONCERT_ENTITY_TYPE.equals(attrs.get("entityType"));
    }

    /**
     * 정수(Integer·Long) entityId만 공연 id로 본다. 7.5 같은 소수는 longValue로 다른 공연과 맞춰지지 않게 null로 둔다.
     */
    private static Long concertIdOf(Map<?, ?> node) {
        Map<?, ?> attrs = (Map<?, ?>) node.get("attrs");
        Object entityId = attrs.get("entityId");
        return entityId instanceof Integer || entityId instanceof Long ? ((Number) entityId).longValue() : null;
    }
}
