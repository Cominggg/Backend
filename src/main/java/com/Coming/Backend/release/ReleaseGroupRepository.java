package com.Coming.Backend.release;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReleaseGroupRepository extends JpaRepository<ReleaseGroup, Long> {

    List<ReleaseGroup> findByArtistId(Long artistId);

    Optional<ReleaseGroup> findByMbid(String mbid);
}
