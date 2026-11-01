package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class SocialProviderUnavailableException extends BusinessException {

    public SocialProviderUnavailableException() {
        super(ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE);
    }
}
