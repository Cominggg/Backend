package com.Coming.Backend.post.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class NotRecommendedException extends BusinessException {

    public NotRecommendedException() {
        super(ErrorCode.NOT_RECOMMENDED);
    }
}
