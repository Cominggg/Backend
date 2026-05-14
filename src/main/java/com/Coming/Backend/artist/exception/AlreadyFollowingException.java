package com.Coming.Backend.artist.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class AlreadyFollowingException extends BusinessException {

    public AlreadyFollowingException() {
        super(ErrorCode.ALREADY_FOLLOWING);
    }
}
