package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record PipelineSetlistCollectResult(
        boolean success,
        @JsonAlias("concert_id") Long concertId,
        @JsonAlias("setlist_fm_id") String setlistFmId,
        @JsonAlias("attribution_url") String attributionUrl,
        List<PipelineTrackResult> tracks,
        String reason
) {
}
