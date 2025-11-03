package com.couponpop.notificationservice.domain.fcmtoken.service;

import com.couponpop.couponpopcoremodule.dto.fcmtoken.request.FcmTokenExpireRequest;

public interface FcmTokenInternalService {

    void expireFcmToken(FcmTokenExpireRequest fcmTokenExpireRequest, Long memberId);
}
