package com.couponpop.notificationservice.domain.notification.dto.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CouponIssuedNotificationPayload(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long ownerId,

        @NotBlank(message = "쿠폰명은 필수입니다.")
        String couponName,

        @NotNull(message = "쿠폰 총 수량은 필수입니다.")
        Integer totalCount,

        @NotNull(message = "발급된 쿠폰 수량은 필수입니다.")
        Integer issuedCount
) {

    public static CouponIssuedNotificationPayload of(Long ownerId, String couponName, Integer totalCount, Integer issuedCount) {
        return new CouponIssuedNotificationPayload(ownerId, couponName, totalCount, issuedCount);
    }
}
