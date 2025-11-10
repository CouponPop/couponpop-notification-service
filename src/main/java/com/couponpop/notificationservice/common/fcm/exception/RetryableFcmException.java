package com.couponpop.notificationservice.common.fcm.exception;

public class RetryableFcmException extends RuntimeException {

    public RetryableFcmException(String message, Throwable cause) {
        super(message, cause);
    }
}
