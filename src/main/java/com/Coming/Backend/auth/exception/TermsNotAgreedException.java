package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class TermsNotAgreedException extends BusinessException {

    public TermsNotAgreedException() {
        super(ErrorCode.TERMS_NOT_AGREED);
    }
}
