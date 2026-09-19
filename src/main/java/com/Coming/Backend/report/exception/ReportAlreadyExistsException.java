package com.Coming.Backend.report.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ReportAlreadyExistsException extends BusinessException {

    public ReportAlreadyExistsException() {
        super(ErrorCode.REPORT_ALREADY_EXISTS);
    }
}
