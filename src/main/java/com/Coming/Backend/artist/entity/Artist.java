package com.Coming.Backend.artist.entity;

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
@Table(name = "artist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mbid", nullable = false, unique = true, length = 36)
    private String mbid;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "sort_name", length = 255)
    private String sortName;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_coming", nullable = false)
    private boolean isComing;

    public void update(String name, String sortName) {
        if (name != null) this.name = name;
        if (sortName != null) this.sortName = sortName;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateIsComing(boolean isComing) {
        this.isComing = isComing;
    }
}
