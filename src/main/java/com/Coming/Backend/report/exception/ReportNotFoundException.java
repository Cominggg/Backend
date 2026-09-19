package com.Coming.Backend.report.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ReportNotFoundException extends BusinessException {

    public ReportNotFoundException() {
        super(ErrorCode.REPORT_NOT_FOUND);
    }
}
