package com.Coming.Backend.release.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.ArtistAlias;
import com.Coming.Backend.artist.entity.UserFollowArtist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistAliasRepository;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
import com.Coming.Backend.common.exception.InvalidInputException;
import com.Coming.Backend.common.response.PageResponse;
import com.Coming.Backend.release.dto.ArtistReleaseItemResponse;
import com.Coming.Backend.release.dto.ReleaseDetailResponse;
import com.Coming.Backend.release.dto.ReleaseListItemResponse;
import com.Coming.Backend.release.dto.TrackDto;
import com.Coming.Backend.release.entity.ReleaseGroup;
import com.Coming.Backend.release.entity.Track;
import com.Coming.Backend.release.exception.ReleaseNotFoundException;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReleaseService {

    private static final List<String> STANDARD_TYPES = List.of("Album", "Single");

    private final ReleaseGroupRepository releaseGroupRepository;
    private final TrackRepository trackRepository;
    private final ArtistRepository artistRepository;
    private final ArtistAliasRepository artistAliasRepository;
    private final UserFollowArtistRepository userFollowArtistRepository;

    /**
     * 아티스트의 디스코그래피를 조회한다. types가 비어 있으면 전체 타입을 반환한다.
     */
    public PageResponse<ArtistReleaseItemResponse> getArtistReleases(Long artistId, List<String> types, Pageable pageable) {
        if (!artistRepository.existsById(artistId)) {
            throw new ArtistNotFoundException();
        }

        Page<ReleaseGroup> page = types.isEmpty()
                ? releaseGroupRepository.findByArtistId(artistId, pageable)
                : releaseGroupRepository.findByArtistIdAndTypeIn(artistId, types, pageable);

        Set<Long> releaseIds = page.stream().map(ReleaseGroup::getId).collect(Collectors.toSet());
        Map<Long, List<TrackDto>> trackMap = trackRepository.findByReleaseGroupIdInOrderByPosition(releaseIds)
                .stream()
                .collect(Collectors.groupingBy(
                        Track::getReleaseGroupId,
                        Collectors.mapping(TrackDto::from, Collectors.toList())
                ));

        return PageResponse.from(page.map(release ->
                ArtistReleaseItemResponse.of(release, trackMap.getOrDefault(release.getId(), List.of()))
        ));
    }

    /**
     * 릴리즈 목록을 q·artistId·type·following 조건으로 조회한다. firstReleaseDate DESC NULLS LAST로 정렬한다.
     *
     * @param q         검색어(릴리즈명·트랙명·아티스트명·alias 대소문자 무시 부분 일치). null·공백이면 텍스트 조건 없이 나머지 필터만 적용한다.
     * @param type      null이면 전체. "Album"·"Single"만 허용하며 그 외 값은 예외를 던진다.
     * @param userId    인증 사용자 ID (미인증이면 null)
     * @param following true이면 팔로우 아티스트 릴리즈만 반환한다(미인증·팔로잉 없으면 빈 페이지). artistId 필터와 조합하지 않으나 type·q 필터는 적용된다
     * @throws InvalidInputException type이 "Album"·"Single"이 아닌 경우
     */
    public PageResponse<ReleaseListItemResponse> getReleases(String q, Long artistId, String type, Long userId, boolean following, Pageable pageable) {
        if (type != null && !STANDARD_TYPES.contains(type)) {
            throw new InvalidInputException();
        }

        Optional<List<Long>> followedResolution = resolveFollowedArtistIds(userId, following);
        if (followedResolution.isPresent() && followedResolution.get().isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
        }
        Long effectiveArtistId = following ? null : artistId;
        List<Long> followedArtistIds = followedResolution.orElse(null);

        String qLike = (q == null || q.isBlank()) ? null : "%" + q.toLowerCase() + "%";
        Pageable sorted = releasesSorted(pageable);

        Page<ReleaseGroup> page = releaseGroupRepository.searchReleases(effectiveArtistId, followedArtistIds, type, qLike, sorted);

        Set<Long> artistIds = page.stream().map(ReleaseGroup::getArtistId).collect(Collectors.toSet());
        Map<Long, String> artistNameMap = artistRepository.findAllById(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Artist::getName));
        Map<Long, String> koreanNameMap = buildKoreanNameMap(List.copyOf(artistIds));

        return PageResponse.from(page.map(release ->
                ReleaseListItemResponse.of(release, artistNameMap.getOrDefault(release.getArtistId(), ""), koreanNameMap.get(release.getArtistId()))
        ));
    }

    /**
     * following 필터에 해당하는 아티스트 ID 목록을 반환한다.
     *
     * @return 필터 미요청 시 {@code Optional.empty()}. 필터 요청 시 팔로우한 아티스트 ID 목록
     *         (미인증·팔로잉 없으면 빈 리스트 — 호출부에서 빈 페이지로 처리해야 함을 의미).
     */
    private Optional<List<Long>> resolveFollowedArtistIds(Long userId, boolean following) {
        if (!following) {
            return Optional.empty();
        }
        if (userId == null) {
            return Optional.of(List.of());
        }
        return Optional.of(userFollowArtistRepository.findByUserId(userId).stream()
                .map(UserFollowArtist::getArtistId)
                .toList());
    }

    /**
     * 릴리즈 상세 정보를 트랙리스트와 함께 조회한다.
     */
    public ReleaseDetailResponse getReleaseDetail(Long id) {
        ReleaseGroup release = releaseGroupRepository.findById(id)
                .orElseThrow(ReleaseNotFoundException::new);

        String artistName = artistRepository.findById(release.getArtistId())
                .map(Artist::getName).orElse("");
        String artistKoreanName = buildKoreanNameMap(List.of(release.getArtistId())).get(release.getArtistId());

        List<TrackDto> tracks = trackRepository.findByReleaseGroupIdOrderByPosition(release.getId())
                .stream().map(TrackDto::from).toList();

        return ReleaseDetailResponse.of(release, artistName, artistKoreanName, tracks);
    }

    private Map<Long, String> buildKoreanNameMap(List<Long> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return artistAliasRepository.findByArtistIdInAndLocale(artistIds, "ko").stream()
                .sorted(Comparator.comparingLong(ArtistAlias::getId))
                .collect(Collectors.toMap(ArtistAlias::getArtistId, ArtistAlias::getName, (existing, replacement) -> existing));
    }

    private Pageable releasesSorted(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("firstReleaseDate").nullsLast())
        );
    }
}
