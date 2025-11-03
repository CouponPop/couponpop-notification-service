package com.couponpop.notificationservice.domain.notification.service;

import com.couponpop.notificationservice.common.fcm.service.FcmSendService;
import com.couponpop.notificationservice.domain.fcmtoken.entity.FcmToken;
import com.couponpop.notificationservice.domain.fcmtoken.repository.FcmTokenRepository;
import com.couponpop.notificationservice.domain.notification.constants.NotificationTemplates;
import com.couponpop.notificationservice.domain.notification.dto.payload.CouponIssuedNotificationPayload;
import com.couponpop.notificationservice.domain.notification.dto.payload.CouponUsedNotificationPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class NotificationService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmSendService fcmSendService;

    /**
     * 쿠폰 사용한 손님에게 푸시 알림 전송
     */
    public void send(@Valid CouponUsedNotificationPayload payload) {

        Long customerId = payload.customerId();
        List<String> tokens = getTokensForMember(customerId);

        String title = NotificationTemplates.COUPON_USED_TITLE.formatted(payload.couponName());
        String body = NotificationTemplates.COUPON_USED_BODY.formatted(
                payload.couponName(),
                payload.storeName()
        );

        for (String token : tokens) {
            fcmSendService.sendNotification(customerId, token, title, body)
                    .exceptionally(throwable -> {
                        log.error("쿠폰 사용 알림 전송 중 오류가 발생했습니다. token={}, message={}", token, throwable.getMessage(), throwable);
                        return null;
                    });
        }
    }

    /**
     * 손님 쿠폰 수령 시 사장님 푸시 알림 전송
     */
    public void send(@Valid CouponIssuedNotificationPayload payload) {

        Long ownerId = payload.ownerId();
        List<String> tokens = getTokensForMember(ownerId);

        String title = NotificationTemplates.COUPON_ISSUED_TITLE.formatted(payload.couponName());
        String body = NotificationTemplates.COUPON_ISSUED_BODY.formatted(
                payload.couponName(),
                payload.totalCount(),
                payload.issuedCount()
        );

        for (String token : tokens) {
            fcmSendService.sendNotification(ownerId, token, title, body)
                    .exceptionally(throwable -> {
                        log.error("쿠폰 수령 알림 전송 중 오류가 발생했습니다. token={}, message={}", token, throwable.getMessage(), throwable);
                        return null;
                    });
        }
    }

    private List<String> getTokensForMember(Long memberId) {

        List<FcmToken> enabledTokens = fcmTokenRepository.findByMemberIdAndNotificationEnabledIsTrue(memberId);
        if (enabledTokens.isEmpty()) {
            log.info("푸시 알림이 활성화된 FCM 토큰이 없어 알림을 건너뜁니다. memberId={}", memberId);
            return List.of();
        }

        // 토큰 중복 제거
        return enabledTokens.stream()
                .map(FcmToken::getFcmToken)
                .distinct()
                .toList();
    }
}
