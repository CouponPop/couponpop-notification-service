package com.couponpop.notificationservice.common.rabbitmq.dto.request;

public record CouponUsageStatsFcmSendRequest(
        Long memberId,
        String token,
        String topDong,
        int topHour,
        int activeEventCount
) {

    public static CouponUsageStatsFcmSendRequest of(Long memberId, String token, String topDong, int topHour, int activeEventCount) {
        return new CouponUsageStatsFcmSendRequest(memberId, token, topDong, topHour, activeEventCount);
    }
}
