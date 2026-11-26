package com.Coming.Backend.post.service;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.repository.UserRepository;
import com.Coming.Backend.auth.service.UserStatusValidator;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.common.util.LikeEscapeUtils;
import com.Coming.Backend.common.util.TiptapConcertMentionFilter;
import com.Coming.Backend.common.util.TiptapContentCodec;
import com.Coming.Backend.common.util.TiptapContentCodec.SanitizedContent;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.dto.PostCreateRequest;
import com.Coming.Backend.post.dto.PostCreateResponse;
import com.Coming.Backend.post.dto.PostDetailResponse;
import com.Coming.Backend.post.dto.PostEntityTagResponse;
import com.Coming.Backend.post.dto.PostSummaryResponse;
import com.Coming.Backend.post.dto.PostUpdateRequest;
import com.Coming.Backend.post.dto.RecommendCountResponse;
import com.Coming.Backend.post.dto.TrendingTagResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.post.entity.Post;
import com.Coming.Backend.post.entity.PostCategory;
import com.Coming.Backend.post.entity.PostEntityTag;
import com.Coming.Backend.post.entity.PostRecommend;
import com.Coming.Backend.post.exception.AlreadyRecommendedException;
import com.Coming.Backend.post.exception.NotRecommendedException;
import com.Coming.Backend.post.exception.PostConcertTagHiddenException;
import com.Coming.Backend.post.exception.PostCoverConcertNotTaggedException;
import com.Coming.Backend.post.exception.PostForbiddenException;
import com.Coming.Backend.post.exception.PostNotFoundException;
import com.Coming.Backend.post.repository.CommentLikeRepository;
import com.Coming.Backend.post.repository.CommentRepository;
import com.Coming.Backend.post.repository.EntityTagCount;
import com.Coming.Backend.post.repository.PostEntityTagRepository;
import com.Coming.Backend.post.repository.PostRecommendRepository;
import com.Coming.Backend.post.repository.PostRepository;
import com.Coming.Backend.post.util.TiptapEntityTagExtractor;
import com.Coming.Backend.post.util.TiptapEntityTagExtractor.EntityTag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostEntityTagRepository postEntityTagRepository;
    private final PostRecommendRepository postRecommendRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final EntityLookupService entityLookupService;
    private final UserStatusValidator userStatusValidator;

    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    @Value("${app.post.popular-board-threshold:10}")
    private long popularBoardThreshold;

    /**
     * 게시글을 생성한다.
     * 본문은 공통 스키마로 검증하고(허용 밖 attrs는 지움), 태그는 본문 언급 카드에서 등장 순서대로 뽑아 저장한다.
     * 숨김 상태(EXCLUDED·PENDING) 공연 카드는 PostConcertTagHiddenException으로 거부한다.
     * 그 밖에 카드가 가리키는 엔티티의 실존 여부는 검증하지 않는다 — 삭제된 참조와 동일하게
     * 조회 시점에 EntityLookupService가 조용히 제외한다.
     * coverConcertId는 본문 공연 카드 중 하나여야 한다.
     */
    @Transactional
    public PostCreateResponse create(Long userId, PostCreateRequest request) {
        userStatusValidator.validateWritable(userId);
        SanitizedContent content = TiptapContentCodec.sanitize(request.content());
        List<EntityTag> tags = TiptapEntityTagExtractor.extract(content.tree());
        Set<Long> taggedConcertIds = concertIdsOf(tags);
        validateConcertsNotHidden(taggedConcertIds);
        if (request.coverConcertId() != null && !taggedConcertIds.contains(request.coverConcertId())) {
            throw new PostCoverConcertNotTaggedException();
        }

        Post post = Post.builder()
                .userId(userId)
                .category(request.category())
                .title(request.title())
                .content(content.json())
                .contentText(content.text())
                .recommendCount(0L)
                .viewCount(0L)
                .commentCount(0L)
                .coverConcertId(request.coverConcertId())
                .build();
        postRepository.save(post);
        saveEntityTags(post.getId(), tags);

        return new PostCreateResponse(post.getId());
    }

    /**
     * 게시글 상세를 조회한다. 조회할 때마다 viewCount가 1 증가한다(중복 조회 방지 없음).
     * 본문의 공연 멘션 중 숨김 상태이거나 없는 공연은 응답에서 뺀다(DB 본문은 그대로 둔다).
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
                removeUnavailableConcertMentions(TiptapContentCodec.read(post.getContent()), entityTags),
                entityTags,
                visibleCoverConcertId(post, entityTags),
                post.getRecommendCount(),
                post.getViewCount() + 1,
                post.getCommentCount(),
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
        ).stream()
                // 탈퇴 회원은 닉네임이 null이라 toMap에 넣을 수 없으므로 제외한다(조회 시 null로 노출).
                .filter(user -> user.getNickname() != null)
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return posts.stream().map(post -> {
            List<PostEntityTagResponse> tags = tagsByPost.getOrDefault(post.getId(), List.of());
            return new PostSummaryResponse(
                    post.getId(),
                    nicknameByUserId.get(post.getUserId()),
                    post.getCategory(),
                    post.getTitle(),
                    tags,
                    visibleCoverConcertId(post, tags),
                    post.getRecommendCount(),
                    post.getViewCount(),
                    post.getCreatedAt()
            );
        }).toList();
    }

    /**
     * 특정 엔티티(공연·아티스트·발매)에 태그된 게시글을 백링크로 조회한다.
     * 숨김 상태 공연이면 빈 페이지를 반환한다.
     *
     * @param sort "recommend"(추천순) 또는 "latest"(최신순). 그 외 값이면 InvalidInputException.
     */
    public PageResponse<PostSummaryResponse> getBacklinks(EntityType entityType, Long entityId, String sort, int page, int size) {
        Sort sortOrder = switch (sort) {
            case "recommend" -> Sort.by(Sort.Direction.DESC, "recommendCount");
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new InvalidInputException();
        };
        if (entityType == EntityType.CONCERT && entityLookupService.hasHiddenConcert(Set.of(entityId))) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Post> result = postRepository.findByEntityTag(entityType, entityId, pageable);
        List<PostSummaryResponse> content = toSummaryResponses(result.getContent());
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /**
     * 최근 N일 이내 작성된 게시글을 추천수 내림차순으로 상위 K개 조회한다.
     */
    public List<PostSummaryResponse> getPopular(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Post> posts = postRepository.findPopularPosts(since, PageRequest.of(0, limit));
        return toSummaryResponses(posts);
    }

    /**
     * 추천수가 임계치 이상인 게시글(인기글)을 최신순으로 조회한다.
     */
    public PageResponse<PostSummaryResponse> getPopularBoard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> result = postRepository.findPopularBoard(popularBoardThreshold, pageable);
        List<PostSummaryResponse> content = toSummaryResponses(result.getContent());
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /**
     * 최근 N일 이내 작성된 게시글에 태그된 엔티티를 언급 빈도 내림차순으로 상위 K개 조회한다.
     */
    public List<TrendingTagResponse> getTrendingTags(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<EntityTagCount> counts = postEntityTagRepository.findTrendingEntityTags(since, ConcertStatus.HIDDEN, PageRequest.of(0, limit));
        if (counts.isEmpty()) {
            return List.of();
        }

        List<EntityLookupService.EntityKey> keys = counts.stream()
                .map(count -> new EntityLookupService.EntityKey(count.entityType(), count.entityId()))
                .toList();
        Map<EntityLookupService.EntityKey, EntityCardResponse> cards = entityLookupService.findCards(keys);

        return counts.stream()
                .filter(count -> cards.containsKey(new EntityLookupService.EntityKey(count.entityType(), count.entityId())))
                .map(count -> {
                    EntityCardResponse card = cards.get(new EntityLookupService.EntityKey(count.entityType(), count.entityId()));
                    return new TrendingTagResponse(count.entityType(), count.entityId(), card.title(), count.count());
                })
                .toList();
    }

    /**
     * 게시글 제목·본문·태그된 엔티티명을 통합 검색한다. 최신순 고정.
     *
     * @param q trim 후 2자 미만이면 InvalidInputException.
     */
    public PageResponse<PostSummaryResponse> search(String q, int page, int size) {
        String trimmedQ = q.trim();
        if (trimmedQ.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new InvalidInputException();
        }
        Pageable pageable = PageRequest.of(page, size);
        String likeQ = LikeEscapeUtils.containsPattern(trimmedQ);
        Page<Post> result = postRepository.searchPosts(likeQ, ConcertStatus.HIDDEN, pageable);
        List<PostSummaryResponse> content = toSummaryResponses(result.getContent());
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /**
     * 게시글을 수정한다. 작성자 본인만 수정할 수 있다.
     * category·title·content는 null이면 기존값을 유지한다.
     * content가 주어지면 공통 스키마로 검증하고, 본문 카드에서 태그를 다시 뽑아 기존 태그를 전체 삭제 후 재삽입한다.
     * 새로 추가한 숨김 상태 공연 카드는 거부하고, 이미 저장돼 있던 태그는 그 사이 공연이 숨겨졌어도 통과시킨다
     * (수정 화면이 같은 카드를 다시 보내므로).
     * coverConcertId가 주어지면 최종 태그(본문에서 뽑은 태그, content가 없으면 저장된 태그)의 공개 공연이어야 한다.
     * 주어지지 않으면 기존 대표를 유지하되, content 수정으로 대표 공연 카드가 빠지면 비운다.
     */
    @Transactional
    public void update(Long userId, Long id, PostUpdateRequest request) {
        userStatusValidator.validateWritable(userId);
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        if (!post.isAuthoredBy(userId)) {
            throw new PostForbiddenException();
        }

        String contentJson = null;
        String contentText = null;
        List<EntityTag> tags = null;
        Set<Long> requestedConcertIds = null;
        if (request.content() != null) {
            SanitizedContent content = TiptapContentCodec.sanitize(request.content());
            contentJson = content.json();
            contentText = content.text();
            tags = TiptapEntityTagExtractor.extract(content.tree());
            requestedConcertIds = concertIdsOf(tags);
        }
        validateConcertTagChanges(id, requestedConcertIds, request.coverConcertId());

        post.update(request.category(), request.title(), contentJson, contentText);

        if (tags != null) {
            postEntityTagRepository.deleteByPostId(id);
            saveEntityTags(id, tags);
        }
        post.applyCoverConcert(requestedConcertIds, request.coverConcertId());
    }

    /**
     * 게시글을 삭제한다. 작성자 본인만 삭제할 수 있다.
     * 게시글이 사라지면 그 밑의 댓글·좋아요·추천은 다시 보여줄 곳이 없으므로 함께 물리 삭제한다.
     */
    @Transactional
    public void delete(Long userId, Long id) {
        userStatusValidator.validateWritable(userId);
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        if (!post.isAuthoredBy(userId)) {
            throw new PostForbiddenException();
        }
        deletePostAndDependents(post);
    }

    /**
     * 관리자가 작성자 검증 없이 게시글을 강제 삭제한다. 신고 처리 등 관리자 조치용.
     */
    @Transactional
    public void adminDelete(Long id) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        deletePostAndDependents(post);
    }

    private void deletePostAndDependents(Post post) {
        Long id = post.getId();
        commentLikeRepository.deleteByCommentPostId(id);
        commentRepository.deleteByPostId(id);
        postRecommendRepository.deleteByPostId(id);
        postEntityTagRepository.deleteByPostId(id);
        postRepository.delete(post);
    }

    /**
     * 게시글을 추천한다. 이미 추천한 게시글이면 AlreadyRecommendedException을 던진다.
     */
    @Transactional
    public RecommendCountResponse recommend(Long userId, Long id) {
        userStatusValidator.validateWritable(userId);
        postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        if (postRecommendRepository.existsByUserIdAndPostId(userId, id)) {
            throw new AlreadyRecommendedException();
        }
        try {
            postRecommendRepository.save(PostRecommend.builder()
                    .userId(userId)
                    .postId(id)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyRecommendedException();
        }
        postRepository.incrementRecommendCount(id);
        return new RecommendCountResponse(postRepository.findRecommendCountById(id));
    }

    /**
     * 게시글 추천을 취소한다. 추천한 적 없으면 NotRecommendedException을 던진다.
     */
    @Transactional
    public RecommendCountResponse unrecommend(Long userId, Long id) {
        userStatusValidator.validateWritable(userId);
        postRepository.findById(id).orElseThrow(PostNotFoundException::new);
        long deletedCount = postRecommendRepository.deleteByUserIdAndPostId(userId, id);
        if (deletedCount == 0) {
            throw new NotRecommendedException();
        }
        postRepository.decrementRecommendCount(id);
        return new RecommendCountResponse(postRepository.findRecommendCountById(id));
    }

    private Set<Long> concertIdsOf(Collection<EntityTag> tags) {
        return tags.stream()
                .filter(tag -> tag.entityType() == EntityType.CONCERT)
                .map(EntityTag::entityId)
                .collect(Collectors.toSet());
    }

    private Set<Long> storedConcertIdsOf(Long postId) {
        return postEntityTagRepository.findByPostId(postId).stream()
                .filter(tag -> tag.getEntityType() == EntityType.CONCERT)
                .map(PostEntityTag::getEntityId)
                .collect(Collectors.toSet());
    }

    private void validateConcertsNotHidden(Set<Long> concertIds) {
        if (entityLookupService.hasHiddenConcert(concertIds)) {
            throw new PostConcertTagHiddenException();
        }
    }

    /**
     * 수정 요청의 공연 태그·대표 공연을 검증한다. requestedConcertIds가 null이면 태그를 바꾸지 않는 요청이다.
     * 새로 추가한 공연만 숨김 여부를 보고, 대표 공연은 최종 태그(본문에서 뽑은 태그, 없으면 저장된 태그)의 공개 공연이어야 한다.
     */
    private void validateConcertTagChanges(Long postId, Set<Long> requestedConcertIds, Long coverConcertId) {
        if (requestedConcertIds == null && coverConcertId == null) {
            return;
        }
        Set<Long> storedConcertIds = storedConcertIdsOf(postId);
        Set<Long> finalConcertIds = requestedConcertIds != null ? requestedConcertIds : storedConcertIds;
        boolean isCoverTagged = coverConcertId != null && finalConcertIds.contains(coverConcertId);
        // 새로 추가한 공연과 대표 공연만 숨김 여부를 본다. 그 사이 숨겨진 기존 태그는 통과시킨다
        Set<Long> concertIdsToCheck = new HashSet<>(finalConcertIds);
        concertIdsToCheck.removeAll(storedConcertIds);
        if (isCoverTagged) {
            concertIdsToCheck.add(coverConcertId);
        }
        validateConcertsNotHidden(concertIdsToCheck);
        if (coverConcertId != null && !isCoverTagged) {
            throw new PostCoverConcertNotTaggedException();
        }
    }

    /**
     * 대표 공연이 응답 태그(숨김·삭제 공연이 빠진 목록)에 있을 때만 id를 반환한다. DB 값은 그대로 둔다.
     */
    private Long visibleCoverConcertId(Post post, List<PostEntityTagResponse> visibleTags) {
        Long coverConcertId = post.getCoverConcertId();
        boolean isVisible = coverConcertId != null && visibleTags.stream()
                .anyMatch(tag -> tag.entityType() == EntityType.CONCERT && coverConcertId.equals(tag.entityId()));
        return isVisible ? coverConcertId : null;
    }

    /**
     * 본문의 공연 멘션 중 숨김 상태이거나 없는 공연의 노드를 뺀다. 응답 태그(이미 공개 공연만 남은 목록)에 있는
     * 공연은 다시 조회하지 않는다.
     */
    private Object removeUnavailableConcertMentions(Object content, List<PostEntityTagResponse> visibleTags) {
        Set<Long> mentionedConcertIds = TiptapConcertMentionFilter.collectConcertIds(content);
        Set<Long> visibleConcertIds = visibleTags.stream()
                .filter(tag -> tag.entityType() == EntityType.CONCERT)
                .map(PostEntityTagResponse::entityId)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> uncheckedConcertIds = new HashSet<>(mentionedConcertIds);
        uncheckedConcertIds.removeAll(visibleConcertIds);
        if (!uncheckedConcertIds.isEmpty()) {
            visibleConcertIds.addAll(entityLookupService.findVisibleConcertIds(uncheckedConcertIds));
        }
        // 유효한 id가 하나도 없어도 필터를 돌려야 id가 null·누락·문자열인 공연 멘션이 빠진다
        return TiptapConcertMentionFilter.removeConcertMentionsExcept(content, visibleConcertIds);
    }

    private void saveEntityTags(Long postId, List<EntityTag> tags) {
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
