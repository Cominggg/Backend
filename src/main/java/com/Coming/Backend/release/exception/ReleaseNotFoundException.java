package com.Coming.Backend.release.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ReleaseNotFoundException extends BusinessException {

    public ReleaseNotFoundException() {
        super(ErrorCode.RELEASE_NOT_FOUND);
    }
}
