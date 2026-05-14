package com.Coming.Backend.concert.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ConcertNotFoundException extends BusinessException {

    public ConcertNotFoundException() {
        super(ErrorCode.CONCERT_NOT_FOUND);
    }
}
