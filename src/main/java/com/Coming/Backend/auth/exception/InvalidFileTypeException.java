package com.Coming.Backend.auth.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class InvalidFileTypeException extends BusinessException {

    public InvalidFileTypeException() {
        super(ErrorCode.INVALID_FILE_TYPE);
    }
}
