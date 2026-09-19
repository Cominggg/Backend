package com.Coming.Backend.report.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ReportDetailRequiredException extends BusinessException {

    public ReportDetailRequiredException() {
        super(ErrorCode.REPORT_DETAIL_REQUIRED);
    }
}
