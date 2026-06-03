package com.Coming.Backend.release.dto;

import com.Coming.Backend.release.entity.Track;

public record TrackDto(
        int position,
        String title,
        Integer lengthMs,
        Integer discNumber,
        Boolean explicit
) {
    public static TrackDto from(Track track) {
        return new TrackDto(
                track.getPosition(),
                track.getTitle(),
                track.getLengthMs(),
                track.getDiscNumber(),
                track.getExplicit()
        );
    }
}
