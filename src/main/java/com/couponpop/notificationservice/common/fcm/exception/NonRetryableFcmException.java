package com.couponpop.notificationservice.common.fcm.exception;

public class NonRetryableFcmException extends RuntimeException {

    public NonRetryableFcmException(String message, Throwable cause) {
        super(message, cause);
    }
}
