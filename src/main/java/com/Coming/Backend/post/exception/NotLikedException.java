package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class NotLikedException extends BusinessException {

    public NotLikedException() {
        super(ErrorCode.NOT_LIKED);
    }
}
