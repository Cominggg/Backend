package com.Coming.Backend.rating.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class RatingNotFoundException extends BusinessException {

    public RatingNotFoundException() {
        super(ErrorCode.RATING_NOT_FOUND);
    }
}
