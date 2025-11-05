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

    // 쿠폰 사용 집계 데이터를 활용한 손님별 맞춤형 알림 템플릿
    public static final String COUPON_USAGE_STATS_TITLE = "%s에 현재 %d개의 쿠폰 이벤트가 진행중입니다";
    public static final String COUPON_USAGE_STATS_BODY = "쿠폰을 가장 많이 사용한 시간대인 %d시에 맞춰 쿠폰 이벤트 소식을 전해드려요.";
}
