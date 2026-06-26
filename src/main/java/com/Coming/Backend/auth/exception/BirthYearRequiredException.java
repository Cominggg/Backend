package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class BirthYearRequiredException extends BusinessException {

    public BirthYearRequiredException() {
        super(ErrorCode.BIRTH_YEAR_REQUIRED);
    }
}
