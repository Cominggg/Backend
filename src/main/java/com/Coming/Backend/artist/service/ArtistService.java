package com.Coming.Backend.artist.service;

import com.Coming.Backend.artist.dto.ArtistConcertResponse;
import com.Coming.Backend.artist.dto.ArtistDetailResponse;
import com.Coming.Backend.artist.dto.ArtistLinkDto;
import com.Coming.Backend.artist.dto.ArtistSummaryResponse;
import com.Coming.Backend.artist.dto.FollowingArtistResponse;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistAlias;
import com.Coming.Backend.artist.entity.ArtistUrl;
import com.Coming.Backend.artist.entity.UserFollowArtist;
import com.Coming.Backend.artist.exception.AlreadyFollowingException;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.exception.NotFollowingException;
import com.Coming.Backend.artist.repository.ArtistAliasRepository;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.ArtistUrlRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.entity.ConcertStatus;
import com.Coming.Backend.concert.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.Coming.Backend.concert.entity.ConcertStatus.EXCLUDED;
import static com.Coming.Backend.concert.entity.ConcertStatus.PENDING;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    public static final String SORT_PROPERTY_FOLLOWER_COUNT = "followerCount";

    private static final LocalDate CONCERT_HISTORY_START = LocalDate.of(2020, 1, 1);
    private static final List<ConcertStatus> HIDDEN_STATUSES = List.of(EXCLUDED, PENDING);

    private final ArtistRepository artistRepository;
    private final ArtistAliasRepository artistAliasRepository;
    private final ArtistUrlRepository artistUrlRepository;
    private final UserFollowArtistRepository userFollowArtistRepository;
    private final ConcertRepository concertRepository;

    /**
     * 아티스트 목록을 조회한다. name, isComing, following 필터를 조합해 적용하며, 기본 정렬은 sortName ASC(동률 시 id ASC)다.
     *
     * @param name      검색 키워드 (null 또는 공백이면 전체 조회)
     * @param isComing  null이면 전체, true/false이면 isComing 필드 기준 필터 적용
     * @param following true이면 팔로잉 아티스트만 반환 (미인증·팔로잉 없으면 빈 페이지)
     * @param pageable  sort=followerCount이면 팔로워 수 기준 전용 집계 쿼리로 처리한다
     * @param userId    인증된 사용자 ID (null이면 isFollowing 항상 false)
     */
    public PageResponse<ArtistSummaryResponse> getArtists(String name, Boolean isComing, Boolean following, Pageable pageable, Long userId) {
        List<Long> followingIds = userId != null
                ? userFollowArtistRepository.findByUserId(userId).stream()
                        .map(UserFollowArtist::getArtistId)
                        .toList()
                : List.of();

        List<Long> filterIds = null;
        if (Boolean.TRUE.equals(following)) {
            if (followingIds.isEmpty()) {
                return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
            }
            filterIds = followingIds;
        }

        boolean hasName = name != null && !name.isBlank();
        Page<Artist> page = fetchArtistsSorted(hasName, isComing, filterIds, name, pageable);

        List<Long> artistIds = page.getContent().stream().map(Artist::getId).toList();
        Map<Long, String> spotifyUrlMap = artistUrlRepository.findByArtistIdInAndTypeIgnoreCase(artistIds, "spotify")
                .stream()
                .collect(Collectors.toMap(ArtistUrl::getArtistId, ArtistUrl::getUrl));
        Map<Long, String> koreanNameMap = buildKoreanNameMap(artistIds);
        Map<Long, Long> followerCountMap = buildFollowerCountMap(artistIds);

        Set<Long> followingIdSet = new HashSet<>(followingIds);
        return PageResponse.from(page.map(artist -> new ArtistSummaryResponse(
                artist.getId(),
                artist.getName(),
                koreanNameMap.get(artist.getId()),
                artist.getImageUrl(),
                artist.isComing(),
                followingIdSet.contains(artist.getId()),
                spotifyUrlMap.get(artist.getId()),
                followerCountMap.getOrDefault(artist.getId(), 0L)
        )));
    }

    private Page<Artist> fetchArtistsSorted(boolean hasName, Boolean isComing, List<Long> ids, String name, Pageable pageable) {
        Optional<Sort.Order> followerCountOrder = pageable.getSort().stream()
                .filter(order -> order.getProperty().equals(SORT_PROPERTY_FOLLOWER_COUNT))
                .findFirst();
        if (followerCountOrder.isPresent()) {
            Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            return artistRepository.findAllOrderByFollowerCount(
                    hasName, name, isComing, ids, followerCountOrder.get().getDirection().isDescending(), unsorted);
        }
        Sort sortWithIdTiebreaker = pageable.getSort().and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortWithIdTiebreaker);
        return fetchArtists(hasName, isComing, ids, name, sorted);
    }

    private Map<Long, Long> buildFollowerCountMap(List<Long> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return userFollowArtistRepository.countByArtistIdIn(artistIds).stream()
                .collect(Collectors.toMap(UserFollowArtistRepository.ArtistFollowerCount::getArtistId,
                        UserFollowArtistRepository.ArtistFollowerCount::getFollowerCount));
    }

    private Page<Artist> fetchArtists(boolean hasName, Boolean isComing, List<Long> ids, String name, Pageable pageable) {
        if (hasName && isComing != null && ids != null) {
            return artistRepository.findByIsComingAndIdInAndNameOrAliasContainingIgnoreCase(isComing, ids, name, pageable);
        }
        if (hasName && isComing != null) {
            return artistRepository.findByIsComingAndNameOrAliasContainingIgnoreCase(isComing, name, pageable);
        }
        if (hasName && ids != null) {
            return artistRepository.findByIdInAndNameOrAliasContainingIgnoreCase(ids, name, pageable);
        }
        if (hasName) {
            return artistRepository.findByNameOrAliasContainingIgnoreCase(name, pageable);
        }
        if (isComing != null && ids != null) {
            return artistRepository.findByIsComingAndIdIn(isComing, ids, pageable);
        }
        if (isComing != null) {
            return artistRepository.findByIsComing(isComing, pageable);
        }
        if (ids != null) {
            return artistRepository.findAllByIdIn(ids, pageable);
        }
        return artistRepository.findAll(pageable);
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
        String koreanName = artistAliasRepository.findFirstByArtistIdAndLocaleOrderByIdAsc(id, "ko")
                .map(ArtistAlias::getName)
                .orElse(null);

        return new ArtistDetailResponse(
                artist.getId(),
                artist.getName(),
                koreanName,
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
                ? concertRepository.findAllByArtistId(artistId, CONCERT_HISTORY_START, HIDDEN_STATUSES, pageable)
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
        try {
            userFollowArtistRepository.save(UserFollowArtist.builder()
                    .userId(userId)
                    .artistId(artistId)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyFollowingException();
        }
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
        Map<Long, String> koreanNameMap = buildKoreanNameMap(artistIds);
        return artistRepository.findAllById(artistIds).stream()
                .map(artist -> new FollowingArtistResponse(
                        artist.getId(),
                        artist.getName(),
                        koreanNameMap.get(artist.getId()),
                        artist.getImageUrl(),
                        artist.isComing(),
                        true
                ))
                .toList();
    }

    private static List<ConcertStatus> toStatuses(String tab) {
        return switch (tab) {
            case "all" -> null;
            case "upcoming" -> List.of(ConcertStatus.UPCOMING, ConcertStatus.ONGOING);
            case "past" -> List.of(ConcertStatus.ENDED, ConcertStatus.CANCELLED);
            default -> throw new InvalidInputException();
        };
    }

    private Map<Long, String> buildKoreanNameMap(List<Long> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return artistAliasRepository.findByArtistIdInAndLocale(artistIds, "ko").stream()
                .sorted(Comparator.comparingLong(ArtistAlias::getId))
                .collect(Collectors.toMap(ArtistAlias::getArtistId, ArtistAlias::getName, (existing, replacement) -> existing));
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
