package com.Coming.Backend.concert.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ConcertNotEndedException extends BusinessException {

    public ConcertNotEndedException() {
        super(ErrorCode.CONCERT_NOT_ENDED);
    }
}
