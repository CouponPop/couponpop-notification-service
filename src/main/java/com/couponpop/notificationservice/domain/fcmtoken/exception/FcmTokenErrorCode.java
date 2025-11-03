package com.couponpop.notificationservice.domain.fcmtoken.exception;

import com.couponpop.notificationservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FcmTokenErrorCode implements ErrorCode {

    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FCM 토큰을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
