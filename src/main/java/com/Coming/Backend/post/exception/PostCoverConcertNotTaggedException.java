package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class PostCoverConcertNotTaggedException extends BusinessException {

    public PostCoverConcertNotTaggedException() {
        super(ErrorCode.POST_COVER_CONCERT_NOT_TAGGED);
    }
}
