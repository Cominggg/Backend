package com.Coming.Backend.notice.exception;

import com.Coming.Backend.common.exception.BusinessException;
import com.Coming.Backend.common.exception.ErrorCode;

public class NoticeNotFoundException extends BusinessException {

    public NoticeNotFoundException() {
        super(ErrorCode.NOTICE_NOT_FOUND);
    }
}
