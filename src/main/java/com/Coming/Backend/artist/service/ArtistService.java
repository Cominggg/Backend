package com.Coming.Backend.artist.service;

import com.Coming.Backend.artist.dto.ArtistConcertResponse;
import com.Coming.Backend.artist.dto.ArtistDetailResponse;
import com.Coming.Backend.artist.dto.ArtistLinkDto;
import com.Coming.Backend.artist.dto.ArtistSummaryResponse;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.UserFollowArtist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.ArtistUrlRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private static final LocalDate CONCERT_HISTORY_START = LocalDate.of(2020, 1, 1);

    private final ArtistRepository artistRepository;
    private final ArtistUrlRepository artistUrlRepository;
    private final UserFollowArtistRepository userFollowArtistRepository;
    private final ConcertRepository concertRepository;

    /**
     * 아티스트 목록을 조회한다. name이 있으면 이름 부분 일치 검색을 적용한다.
     */
    public PageResponse<ArtistSummaryResponse> getArtists(String name, Pageable pageable, Long userId) {
        Page<Artist> page = (name == null || name.isBlank())
                ? artistRepository.findAll(pageable)
                : artistRepository.findByNameContainingIgnoreCase(name, pageable);

        Set<Long> followingIds = resolveFollowingIds(userId);

        return PageResponse.from(page.map(artist -> new ArtistSummaryResponse(
                artist.getId(),
                artist.getName(),
                null,
                artist.isComing(),
                followingIds.contains(artist.getId())
        )));
    }

    /**
     * 아티스트 상세 정보를 조회한다.
     */
    public ArtistDetailResponse getArtist(Long id, Long userId) {
        Artist artist = artistRepository.findById(id).orElseThrow(ArtistNotFoundException::new);

        long followersCount = userFollowArtistRepository.countByArtistId(id);
        boolean isFollowing = userId != null && userFollowArtistRepository.existsByUserIdAndArtistId(userId, id);
        List<ArtistLinkDto> links = artistUrlRepository.findByArtistId(id).stream()
                .map(url -> new ArtistLinkDto(url.getType(), toLabel(url.getType()), url.getUrl()))
                .toList();

        return new ArtistDetailResponse(
                artist.getId(),
                artist.getName(),
                null,
                artist.isComing(),
                isFollowing,
                followersCount,
                artist.getDebutDate(),
                links
        );
    }

    /**
     * 아티스트의 내한 공연 목록을 조회한다. (2020년 이후, tab 필터 적용)
     *
     * @param tab 필터 값: all | upcoming | past
     */
    public PageResponse<ArtistConcertResponse> getArtistConcerts(Long artistId, String tab, Pageable pageable) {
        if (!artistRepository.existsById(artistId)) {
            throw new ArtistNotFoundException();
        }

        List<ConcertStatus> statuses = toStatuses(tab);
        Page<Concert> page = (statuses == null)
                ? concertRepository.findAllByArtistId(artistId, CONCERT_HISTORY_START, pageable)
                : concertRepository.findAllByArtistIdAndStatusIn(artistId, statuses, CONCERT_HISTORY_START, pageable);

        return PageResponse.from(page.map(concert -> new ArtistConcertResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getStartDate(),
                concert.getEndDate(),
                concert.getVenueName(),
                toConcertStatusLabel(concert.getStatus())
        )));
    }

    private Set<Long> resolveFollowingIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return userFollowArtistRepository.findByUserId(userId).stream()
                .map(UserFollowArtist::getArtistId)
                .collect(Collectors.toSet());
    }

    private static List<ConcertStatus> toStatuses(String tab) {
        return switch (tab == null ? "all" : tab) {
            case "upcoming" -> List.of(ConcertStatus.UPCOMING, ConcertStatus.ONGOING);
            case "past" -> List.of(ConcertStatus.ENDED, ConcertStatus.CANCELLED);
            default -> null;
        };
    }

    private static String toLabel(String type) {
        return switch (type.toLowerCase()) {
            case "spotify" -> "Spotify";
            case "youtube" -> "YouTube";
            case "instagram" -> "Instagram";
            case "twitter" -> "Twitter";
            case "facebook" -> "Facebook";
            case "melon" -> "Melon";
            case "bugs" -> "Bugs";
            case "apple_music" -> "Apple Music";
            case "genie" -> "Genie";
            case "flo" -> "FLO";
            case "vibe" -> "Vibe";
            default -> type;
        };
    }

    private static String toConcertStatusLabel(ConcertStatus status) {
        return switch (status) {
            case UPCOMING -> "공연예정";
            case ONGOING -> "공연중";
            case ENDED -> "공연완료";
            case CANCELLED -> "공연취소";
        };
    }
}
