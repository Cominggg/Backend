package com.Coming.Backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PipelineSetlistCollectResult(
        boolean success,
        @JsonProperty("concert_id") Long concertId,
        @JsonProperty("setlist_fm_id") String setlistFmId,
        @JsonProperty("attribution_url") String attributionUrl,
        List<PipelineTrackResult> tracks,
        String reason
) {
}
