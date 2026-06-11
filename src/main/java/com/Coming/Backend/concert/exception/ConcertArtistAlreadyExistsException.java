package com.Coming.Backend.concert.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ConcertArtistAlreadyExistsException extends BusinessException {

    public ConcertArtistAlreadyExistsException() {
        super(ErrorCode.CONCERT_ARTIST_ALREADY_EXISTS);
    }
}
