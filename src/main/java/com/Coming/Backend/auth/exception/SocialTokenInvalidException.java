package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class SocialTokenInvalidException extends BusinessException {

    public SocialTokenInvalidException() {
        super(ErrorCode.SOCIAL_TOKEN_INVALID);
    }
}
