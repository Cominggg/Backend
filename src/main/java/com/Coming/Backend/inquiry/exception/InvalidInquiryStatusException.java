package com.Coming.Backend.inquiry.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class InvalidInquiryStatusException extends BusinessException {

    public InvalidInquiryStatusException() {
        super(ErrorCode.INVALID_INQUIRY_STATUS);
    }
}
