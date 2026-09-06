package com.Coming.Backend.admin.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PipelineTimeoutException extends BusinessException {

    public PipelineTimeoutException() {
        super(ErrorCode.PIPELINE_TIMEOUT);
    }
}
