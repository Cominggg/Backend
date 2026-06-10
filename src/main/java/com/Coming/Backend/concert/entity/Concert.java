package com.Coming.Backend.concert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "concert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Concert {

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

    @Column(name = "poster_url", columnDefinition = "text")
    private String posterUrl;

    @Column(name = "price", columnDefinition = "text")
    private String price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConcertStatus status;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "kopis_update_date", nullable = false)
    private LocalDate kopisUpdateDate;

    @Column(name = "fetch_attempted_at")
    private LocalDateTime fetchAttemptedAt;

    public void markFetchAttempted(LocalDateTime at) {
        this.fetchAttemptedAt = at;
    }

    public void update(String title, String cast, LocalDate startDate, LocalDate endDate,
                       String venueName, String posterUrl, String price) {
        if (title != null) this.title = title;
        if (cast != null) this.cast = cast;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
        if (venueName != null) this.venueName = venueName;
        if (posterUrl != null) this.posterUrl = posterUrl;
        if (price != null) this.price = price;
    }

    public ConcertStatus forceChangeStatus(ConcertStatus newStatus) {
        ConcertStatus previous = this.status;
        this.status = newStatus;
        return previous;
    }
}
