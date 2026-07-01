package com.Coming.Backend.auth.dto;

public record MeResponse(Long id, String nickname, Integer birthYear, String role, boolean agreedMarketing) {
}
