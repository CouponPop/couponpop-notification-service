package com.couponpop.notificationservice.domain.notificationhistory.dto.payload;

import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryStatus;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryType;

public record NotificationHistoryPayload(
        String traceId,
        Long memberId,
        NotificationHistoryType type,
        String title,
        String body,
        NotificationHistoryStatus status,
        String failureReason
) {

    public static NotificationHistoryPayload of(
            String traceId,
            Long memberId,
            NotificationHistoryType type,
            String title,
            String body,
            NotificationHistoryStatus status,
            String failureReason
    ) {

        return new NotificationHistoryPayload(traceId, memberId, type, title, body, status, failureReason);
    }
}
