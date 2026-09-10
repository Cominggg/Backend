package com.Coming.Backend.post.service;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.dto.EntityTagRequest;
import com.Coming.Backend.post.dto.PostCreateRequest;
import com.Coming.Backend.post.dto.PostCreateResponse;
import com.Coming.Backend.post.dto.PostDetailResponse;
import com.Coming.Backend.post.dto.PostEntityTagResponse;
import com.Coming.Backend.post.dto.PostSummaryResponse;
import com.Coming.Backend.post.dto.PostUpdateRequest;
import com.Coming.Backend.post.entity.Post;
import com.Coming.Backend.post.entity.PostCategory;
import com.Coming.Backend.post.entity.PostEntityTag;
import com.Coming.Backend.post.exception.PostForbiddenException;
import com.Coming.Backend.post.exception.PostNotFoundException;
import com.Coming.Backend.post.repository.PostEntityTagRepository;
import com.Coming.Backend.post.repository.PostRecommendRepository;
import com.Coming.Backend.post.repository.PostRepository;
import com.Coming.Backend.post.util.TiptapTextExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostEntityTagRepository postEntityTagRepository;
    private final PostRecommendRepository postRecommendRepository;
    private final UserRepository userRepository;
    private final EntityLookupService entityLookupService;

    /**
     * content 컬럼(jsonb)의 String ↔ Object 변환 전용. Spring이 HTTP 메시지 변환에 사용하는
     * Jackson 인스턴스(3.x)와 무관하게 항상 Jackson 2 databind로 직렬화·역직렬화한다.
     */
    private final ObjectMapper contentObjectMapper = new ObjectMapper();

    /**
     * 게시글을 생성한다. REVIEW·INFO 카테고리는 entityTags가 1개 이상 있어야 한다.
     * entityTags가 가리키는 엔티티의 실존 여부는 검증하지 않는다 — 삭제된 참조와 동일하게
     * 조회 시점에 EntityLookupService가 조용히 제외한다.
     */
    @Transactional
    public PostCreateResponse create(Long userId, PostCreateRequest request) {
        List<EntityTagRequest> tags = request.entityTags() == null ? List.of() : request.entityTags();
        validateEntityTagsRequired(request.category(), tags.size());

        Post post = Post.builder()
                .userId(userId)
                .category(request.category())
                .title(request.title())
                .content(writeContent(request.content()))
                .contentText(TiptapTextExtractor.extract(request.content()))
                .recommendCount(0L)
                .viewCount(0L)
                .build();
        postRepository.save(post);
        saveEntityTags(post.getId(), tags);

        return new PostCreateResponse(post.getId());
    }

    /**
     * 게시글 상세를 조회한다. 조회할 때마다 viewCount가 1 증가한다(중복 조회 방지 없음).
     *
     * @param userId 인증 사용자 ID. null이면 isRecommended는 null, isAuthor는 false로 반환된다.
     */
    @Transactional
    public PostDetailResponse getDetail(Long id, Long userId) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        postRepository.incrementViewCount(id);

        List<PostEntityTag> tags = postEntityTagRepository.findByPostId(id);
        List<PostEntityTagResponse> entityTags = mapEntityTagsByPost(tags).getOrDefault(id, List.of());

        String authorNickname = userRepository.findById(post.getUserId())
                .map(User::getNickname)
                .orElse(null);
        Boolean isRecommended = userId == null ? null : postRecommendRepository.existsByUserIdAndPostId(userId, id);

        return new PostDetailResponse(
                post.getId(),
                authorNickname,
                post.getCategory(),
                post.getTitle(),
                readContent(post.getContent()),
                entityTags,
                post.getRecommendCount(),
                post.getViewCount() + 1,
                isRecommended,
                post.isAuthoredBy(userId),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    /**
     * 게시글 목록을 최신순으로 조회한다.
     *
     * @param category null이면 전체 카테고리
     */
    public PageResponse<PostSummaryResponse> getList(PostCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> result = postRepository.findPosts(category, pageable);
        List<PostSummaryResponse> content = toSummaryResponses(result.getContent());
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /**
     * Post 목록을 PostSummaryResponse 목록으로 변환한다. entityTags·authorNickname을 배치 조회해 채운다.
     * 백링크·통합검색 등 다른 조회 API에서도 재사용한다.
     */
    public List<PostSummaryResponse> toSummaryResponses(List<Post> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, List<PostEntityTagResponse>> tagsByPost =
                mapEntityTagsByPost(postEntityTagRepository.findByPostIdIn(postIds));
        Map<Long, String> nicknameByUserId = userRepository.findAllByIdIn(
                posts.stream().map(Post::getUserId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getNickname));

        return posts.stream().map(post -> new PostSummaryResponse(
                post.getId(),
                nicknameByUserId.get(post.getUserId()),
                post.getCategory(),
                post.getTitle(),
                tagsByPost.getOrDefault(post.getId(), List.of()),
                post.getRecommendCount(),
                post.getViewCount(),
                post.getCreatedAt()
        )).toList();
    }

    /**
     * 게시글을 수정한다. 작성자 본인만 수정할 수 있다.
     * category·title·content는 null이면 기존값을 유지하고, entityTags는 null이면 기존 태그를 유지한다.
     * entityTags가 주어지면 기존 태그를 전체 삭제 후 재삽입한다.
     * 수정 후 최종 category가 REVIEW·INFO면 최종 entityTags가 1개 이상이어야 한다.
     */
    @Transactional
    public void update(Long userId, Long id, PostUpdateRequest request) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        if (!post.isAuthoredBy(userId)) {
            throw new PostForbiddenException();
        }

        PostCategory effectiveCategory = request.category() != null ? request.category() : post.getCategory();
        int effectiveTagCount = request.entityTags() != null
                ? request.entityTags().size()
                : postEntityTagRepository.findByPostId(id).size();
        validateEntityTagsRequired(effectiveCategory, effectiveTagCount);

        String content = request.content() != null ? writeContent(request.content()) : null;
        String contentText = request.content() != null ? TiptapTextExtractor.extract(request.content()) : null;
        post.update(request.category(), request.title(), content, contentText);

        if (request.entityTags() != null) {
            postEntityTagRepository.deleteByPostId(id);
            saveEntityTags(id, request.entityTags());
        }
    }

    /**
     * 게시글을 삭제한다. 작성자 본인만 삭제할 수 있다.
     */
    @Transactional
    public void delete(Long userId, Long id) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        if (!post.isAuthoredBy(userId)) {
            throw new PostForbiddenException();
        }
        postEntityTagRepository.deleteByPostId(id);
        postRepository.delete(post);
    }

    private String writeContent(Object content) {
        try {
            return contentObjectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("게시글 content 직렬화에 실패했습니다.", e);
        }
    }

    private Object readContent(String content) {
        try {
            return contentObjectMapper.readValue(content, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 게시글 content가 유효한 JSON이 아닙니다.", e);
        }
    }

    private void validateEntityTagsRequired(PostCategory category, int tagCount) {
        boolean requiresTags = category == PostCategory.REVIEW || category == PostCategory.INFO;
        if (requiresTags && tagCount == 0) {
            throw new InvalidInputException();
        }
    }

    private void saveEntityTags(Long postId, List<EntityTagRequest> tags) {
        tags.forEach(tag -> postEntityTagRepository.save(PostEntityTag.builder()
                .postId(postId)
                .entityType(tag.entityType())
                .entityId(tag.entityId())
                .build()));
    }

    private Map<Long, List<PostEntityTagResponse>> mapEntityTagsByPost(List<PostEntityTag> tags) {
        if (tags.isEmpty()) {
            return Map.of();
        }
        List<EntityLookupService.EntityKey> keys = tags.stream()
                .map(tag -> new EntityLookupService.EntityKey(tag.getEntityType(), tag.getEntityId()))
                .toList();
        Map<EntityLookupService.EntityKey, EntityCardResponse> cards = entityLookupService.findCards(keys);

        return tags.stream()
                .filter(tag -> cards.containsKey(new EntityLookupService.EntityKey(tag.getEntityType(), tag.getEntityId())))
                .collect(Collectors.groupingBy(
                        PostEntityTag::getPostId,
                        Collectors.mapping(
                                tag -> PostEntityTagResponse.of(tag.getEntityType(), tag.getEntityId(),
                                        cards.get(new EntityLookupService.EntityKey(tag.getEntityType(), tag.getEntityId()))),
                                Collectors.toList())));
    }
}
