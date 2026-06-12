package com.Coming.Backend.concert.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ConcertArtistNotFoundException extends BusinessException {

    public ConcertArtistNotFoundException() {
        super(ErrorCode.CONCERT_ARTIST_NOT_FOUND);
    }
}
