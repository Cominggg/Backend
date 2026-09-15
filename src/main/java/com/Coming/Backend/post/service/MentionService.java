package com.Coming.Backend.post.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.release.entity.ReleaseGroup;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.Coming.Backend.concert.entity.ConcertStatus.EXCLUDED;
import static com.Coming.Backend.concert.entity.ConcertStatus.PENDING;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentionService {

    private static final List<ConcertStatus> HIDDEN_STATUSES = List.of(EXCLUDED, PENDING);

    private final ConcertRepository concertRepository;
    private final ArtistRepository artistRepository;
    private final ReleaseGroupRepository releaseGroupRepository;
    private final EntityLookupService entityLookupService;

    /**
     * 게시글 본문 멘션 자동완성을 위해 type별로 q에 대소문자 무시 부분 일치하는 엔티티를 페이지 단위로 검색한다.
     * 무한 스크롤 조회를 위해 id를 tie-breaker로 사용해 페이지 간 정렬을 안정적으로 유지한다.
     */
    public PageResponse<EntityCardResponse> search(EntityType type, String q, int page, int size) {
        return switch (type) {
            case CONCERT -> searchConcerts(q, PageRequest.of(page, size));
            case ARTIST -> searchArtists(q, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
            case RELEASE -> searchReleases(q, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        };
    }

    private PageResponse<EntityCardResponse> searchConcerts(String q, Pageable pageable) {
        String likeQ = toLikePattern(q);
        Page<Concert> concerts = concertRepository.searchByTitleForMention(HIDDEN_STATUSES, likeQ, pageable);
        return PageResponse.from(concerts.map(entityLookupService::toCard));
    }

    private PageResponse<EntityCardResponse> searchArtists(String q, Pageable pageable) {
        Page<Artist> artists = artistRepository.findByNameOrAliasContainingIgnoreCase(q, pageable);
        return PageResponse.from(artists.map(entityLookupService::toCard));
    }

    private PageResponse<EntityCardResponse> searchReleases(String q, Pageable pageable) {
        Page<ReleaseGroup> releases = releaseGroupRepository
                .searchReleases(null, null, null, toLikePattern(q), pageable);
        Map<Long, String> artistNames = artistRepository.findAllById(
                releases.getContent().stream().map(ReleaseGroup::getArtistId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Artist::getId, Artist::getName));
        return PageResponse.from(
                releases.map(release -> entityLookupService.toCard(release, artistNames.get(release.getArtistId())))
        );
    }

    private String toLikePattern(String q) {
        return "%" + escapeLikeWildcards(q.toLowerCase(Locale.ROOT)) + "%";
    }

    private String escapeLikeWildcards(String q) {
        return q.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
