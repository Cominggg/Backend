package com.Coming.Backend.report.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ReportTargetNotFoundException extends BusinessException {

    public ReportTargetNotFoundException() {
        super(ErrorCode.REPORT_TARGET_NOT_FOUND);
    }
}
