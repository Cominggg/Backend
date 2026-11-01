package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;
import com.Coming.Backend.report.entity.ReportReason;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class UserSuspendedException extends BusinessException {

    /** null이면 영구 정지 */
    private final LocalDateTime suspendedUntil;
    private final ReportReason reason;

    public UserSuspendedException(LocalDateTime suspendedUntil, ReportReason reason) {
        super(ErrorCode.USER_SUSPENDED);
        this.suspendedUntil = suspendedUntil;
        this.reason = reason;
    }
}
