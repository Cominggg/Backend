package com.Coming.Backend.artist.repository;

import com.Coming.Backend.artist.entity.UserFollowArtist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFollowArtistRepository extends JpaRepository<UserFollowArtist, Long> {

    List<UserFollowArtist> findByUserId(Long userId);

    Optional<UserFollowArtist> findByUserIdAndArtistId(Long userId, Long artistId);

    boolean existsByUserIdAndArtistId(Long userId, Long artistId);

    void deleteByUserId(Long userId);
}
