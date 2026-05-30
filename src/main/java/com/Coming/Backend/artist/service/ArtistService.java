package com.Coming.Backend.artist.service;

import com.Coming.Backend.artist.dto.ArtistConcertResponse;
import com.Coming.Backend.artist.dto.ArtistDetailResponse;
import com.Coming.Backend.artist.dto.ArtistLinkDto;
import com.Coming.Backend.artist.dto.ArtistSummaryResponse;
import com.Coming.Backend.artist.dto.FollowingArtistResponse;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.UserFollowArtist;
import com.Coming.Backend.artist.exception.AlreadyFollowingException;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.exception.NotFollowingException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.ArtistUrlRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.common.exception.InvalidInputException;
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
     *
     * @param name   검색 키워드 (null 또는 공백이면 전체 조회)
     * @param userId 인증된 사용자 ID (null이면 isFollowing 항상 false)
     */
    public PageResponse<ArtistSummaryResponse> getArtists(String name, Pageable pageable, Long userId) {
        Page<Artist> page = (name == null || name.isBlank())
                ? artistRepository.findAll(pageable)
                : artistRepository.findByNameOrAliasContainingIgnoreCase(name, pageable);

        Set<Long> followingIds = resolveFollowingIds(userId);

        return PageResponse.from(page.map(artist -> new ArtistSummaryResponse(
                artist.getId(),
                artist.getName(),
                artist.getImageUrl(),
                artist.isComing(),
                followingIds.contains(artist.getId())
        )));
    }

    /**
     * 아티스트 상세 정보를 조회한다.
     *
     * @param userId 인증된 사용자 ID (null이면 isFollowing false)
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
                artist.getImageUrl(),
                artist.isComing(),
                isFollowing,
                followersCount,
                links
        );
    }

    /**
     * 아티스트의 내한 공연 목록을 조회한다. 2020년 이후 공연만 반환한다.
     *
     * @param tab 공연 상태 필터 — all | upcoming | past
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
                concert.getStatus().name()
        )));
    }

    /**
     * 아티스트를 팔로우한다. 이미 팔로우 중이면 AlreadyFollowingException을 던진다.
     */
    @Transactional
    public void follow(Long userId, Long artistId) {
        if (!artistRepository.existsById(artistId)) {
            throw new ArtistNotFoundException();
        }
        if (userFollowArtistRepository.existsByUserIdAndArtistId(userId, artistId)) {
            throw new AlreadyFollowingException();
        }
        userFollowArtistRepository.save(UserFollowArtist.builder()
                .userId(userId)
                .artistId(artistId)
                .build());
    }

    /**
     * 아티스트 팔로우를 취소한다. 팔로우 중이 아니면 NotFollowingException을 던진다.
     */
    @Transactional
    public void unfollow(Long userId, Long artistId) {
        UserFollowArtist follow = userFollowArtistRepository
                .findByUserIdAndArtistId(userId, artistId)
                .orElseThrow(NotFollowingException::new);
        userFollowArtistRepository.delete(follow);
    }

    /**
     * 사용자가 팔로우 중인 아티스트 목록을 반환한다.
     */
    public List<FollowingArtistResponse> getFollowingArtists(Long userId) {
        List<Long> artistIds = userFollowArtistRepository.findByUserId(userId).stream()
                .map(UserFollowArtist::getArtistId)
                .toList();
        return artistRepository.findAllById(artistIds).stream()
                .map(artist -> new FollowingArtistResponse(
                        artist.getId(),
                        artist.getName(),
                        artist.getImageUrl(),
                        artist.isComing()
                ))
                .toList();
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
        return switch (tab) {
            case "all" -> null;
            case "upcoming" -> List.of(ConcertStatus.UPCOMING, ConcertStatus.ONGOING);
            case "past" -> List.of(ConcertStatus.ENDED, ConcertStatus.CANCELLED);
            default -> throw new InvalidInputException();
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

}
