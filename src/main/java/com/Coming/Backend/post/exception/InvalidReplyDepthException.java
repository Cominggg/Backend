package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class InvalidReplyDepthException extends BusinessException {

    public InvalidReplyDepthException() {
        super(ErrorCode.INVALID_REPLY_DEPTH);
    }
}
