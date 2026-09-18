package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PostContentTooLongException extends BusinessException {

    public PostContentTooLongException() {
        super(ErrorCode.POST_CONTENT_TOO_LONG);
    }
}
