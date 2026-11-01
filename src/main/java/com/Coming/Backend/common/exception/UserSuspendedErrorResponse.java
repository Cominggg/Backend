package com.Coming.Backend.common.exception;

import com.Coming.Backend.auth.exception.UserSuspendedException;
import com.Coming.Backend.report.entity.ReportReason;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * USER_SUSPENDED 전용 에러 응답. 공통 형식(code, message)에 정지 만료 시각(KST, null이면 영구 정지)과 사유를 더한다.
 */
public record UserSuspendedErrorResponse(
        String code,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime suspendedUntil,
        ReportReason reason
) {

    public static UserSuspendedErrorResponse of(UserSuspendedException e) {
        ErrorCode errorCode = e.getErrorCode();
        return new UserSuspendedErrorResponse(errorCode.name(), errorCode.getMessage(), e.getSuspendedUntil(), e.getReason());
    }
}
