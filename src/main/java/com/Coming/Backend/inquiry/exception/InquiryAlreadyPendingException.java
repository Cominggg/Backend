package com.Coming.Backend.inquiry.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class InquiryAlreadyPendingException extends BusinessException {

    public InquiryAlreadyPendingException() {
        super(ErrorCode.INQUIRY_ALREADY_PENDING);
    }
}
