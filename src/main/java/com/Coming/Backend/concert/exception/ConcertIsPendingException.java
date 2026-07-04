package com.Coming.Backend.concert.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ConcertIsPendingException extends BusinessException {

    public ConcertIsPendingException() {
        super(ErrorCode.CONCERT_IS_PENDING);
    }
}
