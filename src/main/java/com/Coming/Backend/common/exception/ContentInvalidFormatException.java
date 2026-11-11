package com.Coming.Backend.common.exception;

public class ContentInvalidFormatException extends BusinessException {

    public ContentInvalidFormatException() {
        super(ErrorCode.CONTENT_INVALID_FORMAT);
    }
}
