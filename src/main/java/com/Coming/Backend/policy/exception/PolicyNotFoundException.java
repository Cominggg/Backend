package com.Coming.Backend.policy.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PolicyNotFoundException extends BusinessException {

    public PolicyNotFoundException() {
        super(ErrorCode.POLICY_NOT_FOUND);
    }
}
