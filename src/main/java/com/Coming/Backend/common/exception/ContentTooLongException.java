package com.Coming.Backend.common.exception;

public class ContentTooLongException extends BusinessException {

    public ContentTooLongException() {
        super(ErrorCode.CONTENT_TOO_LONG);
    }
}
