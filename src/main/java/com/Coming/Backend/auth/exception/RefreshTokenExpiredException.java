package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class RefreshTokenExpiredException extends BusinessException {

    public RefreshTokenExpiredException() {
        super(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }
}
