package com.Coming.Backend.concert.dto;

import java.util.List;

public record SetlistResponse(List<SetlistTrackDto> tracks, String sourceUrl) {
}
