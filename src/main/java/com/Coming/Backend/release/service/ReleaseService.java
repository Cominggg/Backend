package com.Coming.Backend.release.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.entity.UserFollowArtist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.artist.repository.UserFollowArtistRepository;
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

import java.util.List;
import java.util.Map;
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
     * 전체 릴리즈 목록을 조회한다. firstReleaseDate DESC NULLS LAST로 정렬한다.
     *
     * @param type      "기타"이면 ALBUM·SINGLE·EP 외 타입 전체를 반환한다
     * @param userId    인증 사용자 ID (미인증이면 null)
     * @param following true이면 팔로우 아티스트 릴리즈만 반환한다. artistId 필터와 조합하지 않으나 type 필터는 적용된다
     */
    public PageResponse<ReleaseListItemResponse> getReleases(Long artistId, String type, Long userId, boolean following, Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("firstReleaseDate").nullsLast())
        );
        Page<ReleaseGroup> page = following
                ? queryFollowingReleases(userId, type, sorted)
                : queryReleases(artistId, type, sorted);

        Set<Long> artistIds = page.stream().map(ReleaseGroup::getArtistId).collect(Collectors.toSet());
        Map<Long, String> artistNameMap = artistRepository.findAllById(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Artist::getName));

        return PageResponse.from(page.map(release ->
                ReleaseListItemResponse.of(release, artistNameMap.getOrDefault(release.getArtistId(), ""))
        ));
    }

    /**
     * 릴리즈 상세 정보를 트랙리스트와 함께 조회한다.
     */
    public ReleaseDetailResponse getReleaseDetail(Long id) {
        ReleaseGroup release = releaseGroupRepository.findById(id)
                .orElseThrow(ReleaseNotFoundException::new);

        String artistName = artistRepository.findById(release.getArtistId())
                .map(Artist::getName).orElse("");

        List<TrackDto> tracks = trackRepository.findByReleaseGroupIdOrderByPosition(release.getId())
                .stream().map(TrackDto::from).toList();

        return ReleaseDetailResponse.of(release, artistName, tracks);
    }

    private Page<ReleaseGroup> queryFollowingReleases(Long userId, String type, Pageable pageable) {
        if (userId == null) {
            return Page.empty(pageable);
        }
        List<Long> artistIds = userFollowArtistRepository.findByUserId(userId).stream()
                .map(UserFollowArtist::getArtistId)
                .toList();
        if (artistIds.isEmpty()) {
            return Page.empty(pageable);
        }
        if ("기타".equals(type)) {
            return releaseGroupRepository.findByArtistIdInAndTypeNotIn(artistIds, STANDARD_TYPES, pageable);
        }
        if (type != null) {
            return releaseGroupRepository.findByArtistIdInAndType(artistIds, type, pageable);
        }
        return releaseGroupRepository.findByArtistIdIn(artistIds, pageable);
    }

    private Page<ReleaseGroup> queryReleases(Long artistId, String type, Pageable pageable) {
        boolean isOther = "기타".equals(type);

        if (artistId != null && isOther) {
            return releaseGroupRepository.findByArtistIdAndTypeNotIn(artistId, STANDARD_TYPES, pageable);
        }
        if (artistId != null && type != null) {
            return releaseGroupRepository.findByArtistIdAndType(artistId, type, pageable);
        }
        if (artistId != null) {
            return releaseGroupRepository.findByArtistId(artistId, pageable);
        }
        if (isOther) {
            return releaseGroupRepository.findByTypeNotIn(STANDARD_TYPES, pageable);
        }
        if (type != null) {
            return releaseGroupRepository.findByType(type, pageable);
        }
        return releaseGroupRepository.findAll(pageable);
    }
}
