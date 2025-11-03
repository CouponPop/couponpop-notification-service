package com.couponpop.notificationservice.domain.notification.service;

import com.couponpop.notificationservice.common.fcm.service.FcmSendService;
import com.couponpop.notificationservice.domain.fcmtoken.entity.FcmToken;
import com.couponpop.notificationservice.domain.fcmtoken.repository.FcmTokenRepository;
import com.couponpop.notificationservice.domain.notification.constants.NotificationTemplates;
import com.couponpop.notificationservice.domain.notification.dto.payload.CouponIssuedNotificationPayload;
import com.couponpop.notificationservice.domain.notification.dto.payload.CouponUsedNotificationPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private FcmSendService fcmSendService;

    @InjectMocks
    private NotificationService notificationService;

    @Nested
    @DisplayName("쿠폰 사용한 손님에게 푸시 알림 전송")
    class SendCustomerCouponUsedNotification {

        @Test
        @DisplayName("활성화된 토큰이 없으면 알림 전송을 건너뛴다")
        void notifyCustomerCouponUsed_skip_whenTokensEmpty() {
            // given
            CouponUsedNotificationPayload payload = CouponUsedNotificationPayload.of(1L, "오픈 기념 쿠폰", "스타벅스 강남역점");
            given(fcmTokenRepository.findByMemberIdAndNotificationEnabledIsTrue(payload.customerId())).willReturn(List.of());

            // when
            notificationService.send(payload);

            // then
            verifyNoInteractions(fcmSendService);
        }

        @Test
        @DisplayName("활성화된 토큰에 알림을 전송한다")
        void notifyCustomerCouponUsed_success_sendNotification() {
            // given
            CouponUsedNotificationPayload payload = CouponUsedNotificationPayload.of(1L, "오픈 기념 쿠폰", "스타벅스 강남역점");

            FcmToken token1 = FcmToken.of(payload.customerId(), "token-1", "ANDROID", "device-1", true, LocalDateTime.now());
            FcmToken token2 = FcmToken.of(payload.customerId(), "token-2", "IOS", "device-2", true, LocalDateTime.now());
            FcmToken token3 = FcmToken.of(payload.customerId(), "token-3", "ANDROID", "device-3", true, LocalDateTime.now());
            FcmToken duplicateToken = FcmToken.of(payload.customerId(), "token-1", "ANDROID", "device-4", true, LocalDateTime.now()); // 중복 토큰

            given(fcmTokenRepository.findByMemberIdAndNotificationEnabledIsTrue(payload.customerId())).willReturn(List.of(token1, token2, token3, duplicateToken));
            given(fcmSendService.sendNotification(anyLong(), anyString(), anyString(), anyString())).willReturn(CompletableFuture.completedFuture(null));

            String expectedTitle = NotificationTemplates.COUPON_USED_TITLE.formatted(payload.couponName());
            String expectedBody = NotificationTemplates.COUPON_USED_BODY.formatted(
                    payload.couponName(),
                    payload.storeName()
            );

            // when
            notificationService.send(payload);

            // then
            ArgumentCaptor<String> fcmTokenCaptor = ArgumentCaptor.forClass(String.class);

            then(fcmSendService).should(times(3)).sendNotification(
                    eq(payload.customerId()),
                    fcmTokenCaptor.capture(),
                    eq(expectedTitle),
                    eq(expectedBody)
            );

            assertThat(fcmTokenCaptor.getAllValues()).containsExactly(
                    token1.getFcmToken(),
                    token2.getFcmToken(),
                    token3.getFcmToken()
            );
        }
    }

    @Nested
    @DisplayName("손님 쿠폰 수령 시 사장님 푸시 알림 전송")
    class SendOwnerCouponIssued {

        @Test
        @DisplayName("활성화된 토큰이 없으면 알림 전송을 건너뛴다")
        void notifyOwnerCouponIssued_skip_whenTokensEmpty() {
            // given
            CouponIssuedNotificationPayload payload = CouponIssuedNotificationPayload.of(2L, "오픈 기념 쿠폰", 30, 15);
            given(fcmTokenRepository.findByMemberIdAndNotificationEnabledIsTrue(payload.ownerId())).willReturn(List.of());

            // when
            notificationService.send(payload);

            // then
            verifyNoInteractions(fcmSendService);
        }

        @Test
        @DisplayName("활성화된 토큰에 알림을 전송한다")
        void notifyOwnerCouponIssued_success_sendNotification() {
            // given
            CouponIssuedNotificationPayload payload = CouponIssuedNotificationPayload.of(2L, "오픈 기념 쿠폰", 30, 15);

            FcmToken token1 = FcmToken.of(payload.ownerId(), "token-1", "ANDROID", "device-1", true, LocalDateTime.now());
            FcmToken token2 = FcmToken.of(payload.ownerId(), "token-2", "IOS", "device-2", true, LocalDateTime.now());
            FcmToken token3 = FcmToken.of(payload.ownerId(), "token-3", "ANDROID", "device-3", true, LocalDateTime.now());
            FcmToken duplicateToken = FcmToken.of(payload.ownerId(), "token-1", "ANDROID", "device-4", true, LocalDateTime.now()); // 중복 토큰

            given(fcmTokenRepository.findByMemberIdAndNotificationEnabledIsTrue(payload.ownerId())).willReturn(List.of(token1, token2, token3, duplicateToken));
            given(fcmSendService.sendNotification(anyLong(), anyString(), anyString(), anyString())).willReturn(CompletableFuture.completedFuture(null));

            String expectedTitle = NotificationTemplates.COUPON_ISSUED_TITLE.formatted(payload.couponName());
            String expectedBody = NotificationTemplates.COUPON_ISSUED_BODY.formatted(
                    payload.couponName(),
                    payload.totalCount(),
                    payload.issuedCount()
            );

            // when
            notificationService.send(payload);

            // then
            ArgumentCaptor<String> fcmTokenCaptor = ArgumentCaptor.forClass(String.class);

            then(fcmSendService).should(times(3)).sendNotification(
                    eq(payload.ownerId()),
                    fcmTokenCaptor.capture(),
                    eq(expectedTitle),
                    eq(expectedBody)
            );

            assertThat(fcmTokenCaptor.getAllValues()).containsExactly(
                    token1.getFcmToken(),
                    token2.getFcmToken(),
                    token3.getFcmToken()
            );
        }

        @Test
        @DisplayName("FCM 전송 중 예외가 발생해도 예외를 전파하지 않는다")
        void notifyOwnerCouponIssued_success_ignoreMessagingException() {
            // given
            CouponIssuedNotificationPayload payload = CouponIssuedNotificationPayload.of(2L, "오픈 기념 쿠폰", 30, 15);
            FcmToken token = FcmToken.of(payload.ownerId(), "token-1", "ANDROID", "device-1", true, LocalDateTime.now());

            given(fcmTokenRepository.findByMemberIdAndNotificationEnabledIsTrue(payload.ownerId())).willReturn(List.of(token));

            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("전송 실패"));
            given(fcmSendService.sendNotification(anyLong(), anyString(), anyString(), anyString())).willReturn(failedFuture);

            // when & then
            assertThatCode(() -> notificationService.send(payload)).doesNotThrowAnyException();
        }
    }
}
