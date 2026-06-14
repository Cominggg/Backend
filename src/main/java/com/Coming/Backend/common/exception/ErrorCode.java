package com.Coming.Backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 Refresh Token입니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기가 5MB를 초과합니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "닉네임은 20자를 초과할 수 없습니다."),

    // Artist
    ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 아티스트입니다."),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "이미 팔로우한 아티스트입니다."),
    NOT_FOLLOWING(HttpStatus.BAD_REQUEST, "팔로우하지 않은 아티스트입니다."),

    // Concert
    CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공연입니다."),
    CONCERT_NOT_PENDING(HttpStatus.BAD_REQUEST, "PENDING 상태의 공연이 아닙니다."),
    CONCERT_ARTIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 매핑된 아티스트입니다."),
    CONCERT_ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연에 매핑되지 않은 아티스트입니다."),
    ALREADY_IN_CALENDAR(HttpStatus.CONFLICT, "이미 캘린더에 추가된 공연입니다."),
    NOT_IN_CALENDAR(HttpStatus.BAD_REQUEST, "캘린더에 없는 공연입니다."),

    // Release
    RELEASE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 릴리즈입니다."),

    // Inquiry
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문의입니다."),
    INQUIRY_ALREADY_PENDING(HttpStatus.CONFLICT, "이미 처리 중인 문의가 존재합니다."),
    INVALID_INQUIRY_STATUS(HttpStatus.BAD_REQUEST, "변경 불가능한 문의 상태입니다."),
    TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 대상입니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
