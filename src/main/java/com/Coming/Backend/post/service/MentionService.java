package com.Coming.Backend.post.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.common.util.LikeEscapeUtils;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.release.entity.ReleaseGroup;
import com.Coming.Backend.release.entity.Track;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;
import com.Coming.Backend.release.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
    private final TrackRepository trackRepository;
    private final EntityLookupService entityLookupService;

    /**
     * 게시글 본문 멘션 자동완성을 위해 q에 대소문자 무시 부분 일치하는 엔티티를 검색한다.
     * type이 하나면 그 종류만 페이지 단위로 검색한다(무한 스크롤용).
     * 여러 종류(null·빈 목록이면 전 종류)면 종류별 상위 size개를 합쳐 제목이 q로 시작하는 것을 먼저,
     * 같으면 CONCERT·ARTIST·RELEASE·TRACK 순으로 정렬해 size개를 반환한다. 이때 page는 0만 허용한다.
     *
     * @throws InvalidInputException 여러 종류를 검색하면서 page가 0이 아닐 때
     */
    public PageResponse<EntityCardResponse> search(List<EntityType> types, String q, int page, int size) {
        List<EntityType> requested = types == null || types.isEmpty()
                ? List.of(EntityType.values())
                : types.stream().distinct().sorted().toList();
        if (requested.size() == 1) {
            return searchByType(requested.getFirst(), q, page, size);
        }
        if (page != 0) {
            throw new InvalidInputException();
        }
        String lowerQ = q.toLowerCase(Locale.ROOT);
        List<EntityCardResponse> merged = requested.stream()
                .flatMap(type -> searchByType(type, q, 0, size).content().stream())
                .sorted(Comparator.comparing(card -> !startsWithIgnoreCase(card.title(), lowerQ)))
                .limit(size)
                .toList();
        return new PageResponse<>(merged, 0, size, merged.size(), merged.isEmpty() ? 0 : 1);
    }

    private boolean startsWithIgnoreCase(String title, String lowerQ) {
        return title != null && title.toLowerCase(Locale.ROOT).startsWith(lowerQ);
    }

    /**
     * 무한 스크롤 조회를 위해 id를 tie-breaker로 사용해 페이지 간 정렬을 안정적으로 유지한다.
     */
    private PageResponse<EntityCardResponse> searchByType(EntityType type, String q, int page, int size) {
        return switch (type) {
            case CONCERT -> searchConcerts(q, PageRequest.of(page, size));
            case ARTIST -> searchArtists(q, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
            case RELEASE -> searchReleases(q, PageRequest.of(page, size));
            case TRACK -> searchTracks(q, PageRequest.of(page, size));
        };
    }

    private PageResponse<EntityCardResponse> searchConcerts(String q, Pageable pageable) {
        String likeQ = LikeEscapeUtils.containsPattern(q);
        Page<Concert> concerts = concertRepository.searchByTitleForMention(HIDDEN_STATUSES, likeQ, pageable);
        return PageResponse.from(concerts.map(entityLookupService::toCard));
    }

    private PageResponse<EntityCardResponse> searchArtists(String q, Pageable pageable) {
        Page<Artist> artists = artistRepository.findByNameOrAliasContainingIgnoreCase(LikeEscapeUtils.escape(q), pageable);
        return PageResponse.from(artists.map(entityLookupService::toCard));
    }

    private PageResponse<EntityCardResponse> searchReleases(String q, Pageable pageable) {
        Page<ReleaseGroup> releases = releaseGroupRepository
                .searchByTitleForMention(LikeEscapeUtils.containsPattern(q), pageable);
        Map<Long, String> artistNames = artistRepository.findAllById(
                releases.getContent().stream().map(ReleaseGroup::getArtistId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Artist::getId, Artist::getName));
        return PageResponse.from(
                releases.map(release -> entityLookupService.toCard(release, artistNames.get(release.getArtistId())))
        );
    }

    private PageResponse<EntityCardResponse> searchTracks(String q, Pageable pageable) {
        Page<Track> tracks = trackRepository.searchByTitleForMention(LikeEscapeUtils.containsPattern(q), pageable);
        Map<Long, EntityCardResponse> cardsByTrackId = entityLookupService.toTrackCardsById(tracks.getContent());
        return PageResponse.from(tracks.map(track -> cardsByTrackId.get(track.getId())));
    }
}
