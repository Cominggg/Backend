package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PostContentInvalidFormatException extends BusinessException {

    public PostContentInvalidFormatException() {
        super(ErrorCode.POST_CONTENT_INVALID_FORMAT);
    }
}
