package com.Coming.Backend.calendar.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class NotInCalendarException extends BusinessException {

    public NotInCalendarException() {
        super(ErrorCode.NOT_IN_CALENDAR);
    }
}
