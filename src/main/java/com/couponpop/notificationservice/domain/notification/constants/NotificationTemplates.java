package com.couponpop.notificationservice.domain.notification.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationTemplates {

    // 손님 쿠폰 수령 시 사장님 푸시 알림 템플릿
    public static final String COUPON_ISSUED_TITLE = "[%s] 쿠폰을 손님이 수령했습니다.";
    public static final String COUPON_ISSUED_BODY = """
            쿠폰명: %s
            쿠폰 총 수량: %d
            발급된 쿠폰 수량: %d
            """;

    // 쿠폰 사용한 손님에게 푸시 알림 템플릿
    public static final String COUPON_USED_TITLE = "[%s] 쿠폰이 사용되었습니다.";
    public static final String COUPON_USED_BODY = """
            쿠폰명: %s
            사용 매장: %s
            """;
}
