package com.Coming.Backend.rating.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class RatingTargetNotFoundException extends BusinessException {

    public RatingTargetNotFoundException() {
        super(ErrorCode.RATING_TARGET_NOT_FOUND);
    }
}
