package com.Coming.Backend.common.exception;

public class InvalidInputException extends BusinessException {

    public InvalidInputException() {
        super(ErrorCode.INVALID_INPUT);
    }
}
