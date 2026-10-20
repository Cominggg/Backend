package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class UserSuspendedException extends BusinessException {

    public UserSuspendedException() {
        super(ErrorCode.USER_SUSPENDED);
    }
}
