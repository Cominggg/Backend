package com.Coming.Backend.auth.dev;

import com.Coming.Backend.auth.entity.UserRole;

public record DevLoginRequest(String nickname, UserRole role) {
}
