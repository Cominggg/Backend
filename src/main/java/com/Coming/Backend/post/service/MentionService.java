package com.Coming.Backend.post.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.release.entity.ReleaseGroup;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * 게시글 본문 멘션 자동완성을 위해 type별로 q에 대소문자 무시 부분 일치하는 엔티티를 최대 limit건 검색한다.
     */
    public List<EntityCardResponse> search(EntityType type, String q, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return switch (type) {
            case CONCERT -> searchConcerts(q, pageable);
            case ARTIST -> searchArtists(q, pageable);
            case RELEASE -> searchReleases(q, pageable);
        };
    }

    private List<EntityCardResponse> searchConcerts(String q, Pageable pageable) {
        String likeQ = toLikePattern(q);
        return concertRepository.searchByTitleForMention(HIDDEN_STATUSES, likeQ, pageable).stream()
                .map(entityLookupService::toCard)
                .toList();
    }

    private List<EntityCardResponse> searchArtists(String q, Pageable pageable) {
        return artistRepository.findByNameOrAliasContainingIgnoreCase(q, pageable).stream()
                .map(entityLookupService::toCard)
                .toList();
    }

    private List<EntityCardResponse> searchReleases(String q, Pageable pageable) {
        List<ReleaseGroup> releases = releaseGroupRepository
                .searchReleases(null, null, null, toLikePattern(q), pageable)
                .getContent();
        Map<Long, String> artistNames = artistRepository.findAllById(
                releases.stream().map(ReleaseGroup::getArtistId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Artist::getId, Artist::getName));
        return releases.stream()
                .map(release -> entityLookupService.toCard(release, artistNames.get(release.getArtistId())))
                .toList();
    }

    private String toLikePattern(String q) {
        return "%" + q.toLowerCase(Locale.ROOT) + "%";
    }
}
