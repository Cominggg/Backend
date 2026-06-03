package com.Coming.Backend.release.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "release_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReleaseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mbid", unique = true, length = 36)
    private String mbid;

    @Column(name = "spotify_id", unique = true, length = 22)
    private String spotifyId;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "first_release_date")
    private LocalDate firstReleaseDate;

    @Column(name = "cover_url", columnDefinition = "text")
    private String coverUrl;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "total_tracks")
    private Integer totalTracks;
}
