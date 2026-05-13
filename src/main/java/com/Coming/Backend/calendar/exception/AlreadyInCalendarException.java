package com.Coming.Backend.calendar.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class AlreadyInCalendarException extends BusinessException {

    public AlreadyInCalendarException() {
        super(ErrorCode.ALREADY_IN_CALENDAR);
    }
}
