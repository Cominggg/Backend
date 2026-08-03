package com.Coming.Backend.artist.repository;

import com.Coming.Backend.artist.entity.ArtistUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArtistUrlRepository extends JpaRepository<ArtistUrl, Long> {

    List<ArtistUrl> findByArtistId(Long artistId);

    List<ArtistUrl> findByArtistIdInAndTypeIgnoreCase(List<Long> artistIds, String type);

    @Modifying
    @Query("DELETE FROM ArtistUrl au WHERE au.artistId = :artistId")
    void deleteByArtistId(@Param("artistId") Long artistId);
}
