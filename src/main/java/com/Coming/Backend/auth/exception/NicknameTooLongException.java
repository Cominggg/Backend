package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class NicknameTooLongException extends BusinessException {

    public NicknameTooLongException() {
        super(ErrorCode.NICKNAME_TOO_LONG);
    }
}
