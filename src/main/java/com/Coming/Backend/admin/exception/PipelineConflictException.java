package com.Coming.Backend.admin.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PipelineConflictException extends BusinessException {

    public PipelineConflictException() {
        super(ErrorCode.PIPELINE_CONFLICT);
    }
}
