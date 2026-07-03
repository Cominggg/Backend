package com.Coming.Backend.admin.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PipelineNotFoundException extends BusinessException {

    public PipelineNotFoundException() {
        super(ErrorCode.PIPELINE_NOT_FOUND);
    }
}
