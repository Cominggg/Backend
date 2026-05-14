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

@Entity
@Table(name = "track")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_group_id", nullable = false)
    private Long releaseGroupId;

    @Column(name = "mbid", nullable = false, unique = true, length = 36)
    private String mbid;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "length_ms")
    private Integer lengthMs;
}
