package com.Coming.Backend.concert.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ConcertNotPendingException extends BusinessException {

    public ConcertNotPendingException() {
        super(ErrorCode.CONCERT_NOT_PENDING);
    }
}
