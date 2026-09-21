package com.Coming.Backend.rating.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class InvalidRatingScoreException extends BusinessException {

    public InvalidRatingScoreException() {
        super(ErrorCode.INVALID_RATING_SCORE);
    }
}
