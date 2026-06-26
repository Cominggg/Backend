package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class NicknameRequiredException extends BusinessException {

    public NicknameRequiredException() {
        super(ErrorCode.NICKNAME_REQUIRED);
    }
}
