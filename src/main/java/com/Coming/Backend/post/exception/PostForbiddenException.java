package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PostForbiddenException extends BusinessException {

    public PostForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
