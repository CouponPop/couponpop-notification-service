package com.couponpop.notificationservice.domain.notification.dto.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CouponUsedNotificationPayload(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long customerId,

        @NotBlank(message = "쿠폰명은 필수입니다.")
        String couponName,

        @NotBlank(message = "매장명은 필수입니다.")
        String storeName
) {

    public static CouponUsedNotificationPayload of(Long customerId, String couponName, String storeName) {
        return new CouponUsedNotificationPayload(customerId, couponName, storeName);
    }
}
