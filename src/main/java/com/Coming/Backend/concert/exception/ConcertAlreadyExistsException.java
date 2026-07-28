package com.Coming.Backend.concert.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ConcertAlreadyExistsException extends BusinessException {

    public ConcertAlreadyExistsException() {
        super(ErrorCode.CONCERT_ALREADY_EXISTS);
    }
}
