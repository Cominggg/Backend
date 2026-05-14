package com.Coming.Backend.concert.entity;

import com.Coming.Backend.common.entity.BaseTimeEntity;
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
@Table(name = "concert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Concert extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kopis_id", nullable = false, unique = true, length = 50)
    private String kopisId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "\"cast\"", columnDefinition = "text")
    private String cast;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "venue_name", nullable = false, length = 255)
    private String venueName;

    @Column(name = "venue_address", length = 500)
    private String venueAddress;

    @Column(name = "poster_url", columnDefinition = "text")
    private String posterUrl;

    @Column(name = "price", columnDefinition = "text")
    private String price;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "kopis_update_date", nullable = false)
    private LocalDate kopisUpdateDate;
}
