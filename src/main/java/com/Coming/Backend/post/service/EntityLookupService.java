package com.Coming.Backend.post.service;

import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.repository.ArtistRepository;
import com.Coming.Backend.concert.entity.Concert;
import com.Coming.Backend.concert.repository.ConcertRepository;
import com.Coming.Backend.post.dto.EntityCardResponse;
import com.Coming.Backend.post.entity.EntityType;
import com.Coming.Backend.release.entity.ReleaseGroup;
import com.Coming.Backend.release.repository.ReleaseGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntityLookupService {

    private final ConcertRepository concertRepository;
    private final ArtistRepository artistRepository;
    private final ReleaseGroupRepository releaseGroupRepository;

    /**
     * entityType·entityId 키 목록에 대응하는 엔티티 카드 정보를 일괄 조회한다.
     * 참조 대상이 삭제된 경우 결과 Map에서 해당 키가 제외된다.
     */
    public Map<EntityKey, EntityCardResponse> findCards(Collection<EntityKey> keys) {
        Map<EntityKey, EntityCardResponse> result = new HashMap<>();

        concertRepository.findAllById(idsOf(keys, EntityType.CONCERT)).forEach(concert ->
                result.put(new EntityKey(EntityType.CONCERT, concert.getId()), toCard(concert)));

        artistRepository.findAllById(idsOf(keys, EntityType.ARTIST)).forEach(artist ->
                result.put(new EntityKey(EntityType.ARTIST, artist.getId()), toCard(artist)));

        List<ReleaseGroup> releases = releaseGroupRepository.findAllById(idsOf(keys, EntityType.RELEASE));
        Map<Long, String> artistNames = artistRepository.findAllById(
                releases.stream().map(ReleaseGroup::getArtistId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Artist::getId, Artist::getName));
        releases.forEach(release ->
                result.put(new EntityKey(EntityType.RELEASE, release.getId()),
                        toCard(release, artistNames.get(release.getArtistId()))));

        return result;
    }

    private Set<Long> idsOf(Collection<EntityKey> keys, EntityType type) {
        return keys.stream()
                .filter(key -> key.type() == type)
                .map(EntityKey::id)
                .collect(Collectors.toSet());
    }

    private EntityCardResponse toCard(Concert concert) {
        String subtitle = concert.getStartDate() + " · " + concert.getVenueName();
        return new EntityCardResponse(EntityType.CONCERT, concert.getId(), concert.getTitle(), subtitle, concert.getPosterUrl());
    }

    private EntityCardResponse toCard(Artist artist) {
        return new EntityCardResponse(EntityType.ARTIST, artist.getId(), artist.getName(), null, artist.getImageUrl());
    }

    private EntityCardResponse toCard(ReleaseGroup release, String artistName) {
        return new EntityCardResponse(EntityType.RELEASE, release.getId(), release.getTitle(), artistName, release.getCoverUrl());
    }

    public record EntityKey(EntityType type, Long id) {
    }
}
