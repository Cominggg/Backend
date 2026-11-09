package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PostConcertTagHiddenException extends BusinessException {

    public PostConcertTagHiddenException() {
        super(ErrorCode.POST_CONCERT_TAG_HIDDEN);
    }
}
