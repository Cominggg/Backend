package com.Coming.Backend.artist.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class NotFollowingException extends BusinessException {

    public NotFollowingException() {
        super(ErrorCode.NOT_FOLLOWING);
    }
}
