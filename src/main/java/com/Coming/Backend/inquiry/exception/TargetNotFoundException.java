package com.Coming.Backend.inquiry.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class TargetNotFoundException extends BusinessException {

    public TargetNotFoundException() {
        super(ErrorCode.TARGET_NOT_FOUND);
    }
}
