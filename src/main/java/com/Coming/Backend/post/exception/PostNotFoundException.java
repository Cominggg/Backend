package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PostNotFoundException extends BusinessException {

    public PostNotFoundException() {
        super(ErrorCode.POST_NOT_FOUND);
    }
}
