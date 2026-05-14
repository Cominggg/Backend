package com.Coming.Backend.artist.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class ArtistNotFoundException extends BusinessException {

    public ArtistNotFoundException() {
        super(ErrorCode.ARTIST_NOT_FOUND);
    }
}
