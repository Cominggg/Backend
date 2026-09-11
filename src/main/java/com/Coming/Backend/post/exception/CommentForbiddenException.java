package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class CommentForbiddenException extends BusinessException {

    public CommentForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
