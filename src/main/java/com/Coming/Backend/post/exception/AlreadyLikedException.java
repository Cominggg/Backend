package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class AlreadyLikedException extends BusinessException {

    public AlreadyLikedException() {
        super(ErrorCode.ALREADY_LIKED);
    }
}
