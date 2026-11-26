package com.Coming.Backend.post.util;

import com.Coming.Backend.common.exception.ContentInvalidFormatException;
import com.Coming.Backend.post.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 검증을 마친 게시글 본문(TiptapContentValidator 결과)에서 언급 카드를 게시글 태그로 뽑는다.
 * 본문에 처음 나온 순서를 지키고 같은 (entityType, entityId)는 하나로 센다. 고유 카드가 10개를 넘으면 거부한다.
 */
public final class TiptapEntityTagExtractor {

    private static final int MAX_TAG_COUNT = 10;

    private TiptapEntityTagExtractor() {
    }

    public record EntityTag(EntityType entityType, Long entityId) {
    }

    public static List<EntityTag> extract(Object content) {
        Set<EntityTag> tags = new LinkedHashSet<>();
        collect(content, tags);
        if (tags.size() > MAX_TAG_COUNT) {
            throw new ContentInvalidFormatException();
        }
        return new ArrayList<>(tags);
    }

    private static void collect(Object node, Set<EntityTag> tags) {
        if (node instanceof List<?> list) {
            list.forEach(child -> collect(child, tags));
            return;
        }
        if (!(node instanceof Map<?, ?> map)) {
            return;
        }
        if ("entityMentionCard".equals(map.get("type")) && map.get("attrs") instanceof Map<?, ?> attrs) {
            tags.add(new EntityTag(
                    EntityType.valueOf((String) attrs.get("entityType")),
                    ((Number) attrs.get("entityId")).longValue()));
            return;
        }
        collect(map.get("content"), tags);
    }
}
