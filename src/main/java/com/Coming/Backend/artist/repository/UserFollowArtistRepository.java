package com.Coming.Backend.artist.repository;

import com.Coming.Backend.artist.entity.UserFollowArtist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserFollowArtistRepository extends JpaRepository<UserFollowArtist, Long> {

    List<UserFollowArtist> findByUserId(Long userId);

    Optional<UserFollowArtist> findByUserIdAndArtistId(Long userId, Long artistId);

    boolean existsByUserIdAndArtistId(Long userId, Long artistId);

    void deleteByUserId(Long userId);

    long countByArtistId(Long artistId);

    @Query("SELECT ufa.artistId AS artistId, COUNT(ufa) AS followerCount FROM UserFollowArtist ufa WHERE ufa.artistId IN :artistIds GROUP BY ufa.artistId")
    List<ArtistFollowerCount> countByArtistIdIn(@Param("artistIds") List<Long> artistIds);

    interface ArtistFollowerCount {
        Long getArtistId();

        Long getFollowerCount();
    }
}
