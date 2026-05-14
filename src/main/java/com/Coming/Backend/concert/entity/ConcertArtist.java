package com.Coming.Backend.concert.entity;

import com.Coming.Backend.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "concert_artist", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"concert_id", "artist_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ConcertArtist extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "concert_id", nullable = false)
    private Long concertId;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(name = "confidence", nullable = false, length = 10)
    private String confidence;

    @Column(name = "matched_by", nullable = false, length = 20)
    private String matchedBy;
}
