package com.Coming.Backend.admin.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PipelineServerException extends BusinessException {

    public PipelineServerException() {
        super(ErrorCode.PIPELINE_SERVER_ERROR);
    }
}
