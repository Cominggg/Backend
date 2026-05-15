package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class RefreshTokenInvalidException extends BusinessException {

    public RefreshTokenInvalidException() {
        super(ErrorCode.REFRESH_TOKEN_INVALID);
    }
}
