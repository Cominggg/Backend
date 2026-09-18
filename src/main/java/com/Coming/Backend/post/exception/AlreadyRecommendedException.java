package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class AlreadyRecommendedException extends BusinessException {

    public AlreadyRecommendedException() {
        super(ErrorCode.ALREADY_RECOMMENDED);
    }
}
