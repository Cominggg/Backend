package com.Coming.Backend.admin.service;

import com.Coming.Backend.admin.dto.AdminArtistCreateRequest;
import com.Coming.Backend.admin.dto.AdminArtistUpdateRequest;
import com.Coming.Backend.artist.entity.Artist;
import com.Coming.Backend.artist.exception.ArtistNotFoundException;
import com.Coming.Backend.artist.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ArtistRepository artistRepository;

    /**
     * 아티스트를 수동 등록한다.
     */
    @Transactional
    public void createArtist(AdminArtistCreateRequest request) {
        artistRepository.save(Artist.builder()
                .mbid(request.mbid())
                .name(request.name())
                .sortName(request.sortName())
                .debutDate(request.debutDate())
                .isComing(false)
                .build());
    }

    /**
     * 아티스트 정보를 수정한다. 존재하지 않는 아티스트 ID이면 ArtistNotFoundException을 던진다.
     */
    @Transactional
    public void updateArtist(Long id, AdminArtistUpdateRequest request) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(ArtistNotFoundException::new);
        artist.update(request.name(), request.sortName(), request.debutDate());
    }
}
