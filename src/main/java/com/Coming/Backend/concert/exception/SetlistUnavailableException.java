package com.Coming.Backend.concert.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class SetlistUnavailableException extends BusinessException {

    public SetlistUnavailableException() {
        super(ErrorCode.SETLIST_TEMPORARILY_UNAVAILABLE);
    }
}
