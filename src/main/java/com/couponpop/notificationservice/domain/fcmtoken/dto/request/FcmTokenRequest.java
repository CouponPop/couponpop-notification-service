package com.couponpop.notificationservice.domain.fcmtoken.dto.request;

import lombok.Builder;

@Builder
public record FcmTokenRequest(
        String fcmToken,
        String deviceType,
        String deviceIdentifier
) {
}
