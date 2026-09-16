package com.Coming.Backend.policy.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PolicyVersionDuplicateException extends BusinessException {

    public PolicyVersionDuplicateException() {
        super(ErrorCode.POLICY_VERSION_DUPLICATE);
    }
}
