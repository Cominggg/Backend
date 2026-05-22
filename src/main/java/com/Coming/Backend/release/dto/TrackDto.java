package com.Coming.Backend.release.dto;

import com.Coming.Backend.release.entity.Track;

public record TrackDto(
        int position,
        String title,
        Integer length_ms
) {
    public static TrackDto from(Track track) {
        return new TrackDto(track.getPosition(), track.getTitle(), track.getLengthMs());
    }
}
