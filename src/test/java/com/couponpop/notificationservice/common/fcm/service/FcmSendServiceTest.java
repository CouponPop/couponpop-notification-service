package com.couponpop.notificationservice.common.fcm.service;

import com.couponpop.notificationservice.common.fcm.exception.NonRetryableFcmException;
import com.couponpop.notificationservice.common.fcm.exception.RetryableFcmException;
import com.couponpop.notificationservice.common.fcm.factory.FcmMessageFactory;
import com.couponpop.notificationservice.domain.fcmtoken.service.FcmTokenService;
import com.couponpop.notificationservice.domain.notification.service.NotificationIdempotencyService;
import com.couponpop.notificationservice.domain.notificationhistory.dto.payload.NotificationHistoryPayload;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryStatus;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryType;
import com.couponpop.notificationservice.domain.notificationhistory.service.NotificationHistoryService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmSendServiceTest {

    @Mock
    private FcmMessageFactory fcmMessageFactory;

    @Mock
    private NotificationHistoryService notificationHistoryService;

    @Mock
    private NotificationIdempotencyService notificationIdempotencyService;

    @Mock
    private FcmTokenService fcmTokenService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Spy
    // execute가 실제로 Runnable.run()을 실행해 줘야만 콜백 내부 로직이 동작하고 이후 검증이 이어지므로
    // Spy 사용 (직접 구현한 동기 Executor를 주입한다)
    private Executor fcmTaskExecutor = new DirectExecutor();

    @InjectMocks
    private FcmSendService fcmSendService;

    // sendAsync 콜백이 별도 스레드에서 실행되면 테스트 검증이 어려우므로,
    // 테스트에서는 비동기 작업을 즉시 실행해 동기적으로 처리한다.
    private static class DirectExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    @Nested
    @DisplayName("비동기 푸시 알림 전송")
    class SendNotification {

        @Test
        @DisplayName("전송 성공 시 토큰을 갱신하고 히스토리를 성공으로 저장한다")
        void sendNotification_success_whenRequestValid() throws FirebaseMessagingException {
            // given
            String traceId = "trace-id";
            Long memberId = 1L;
            String token = "success-token";
            String title = "제목";
            String body = "본문";

            Message message = Message.builder().setToken(token).build();
            when(fcmMessageFactory.createMessage(token, title, body)).thenReturn(message);
            when(notificationIdempotencyService.acquireProcessingKey(traceId)).thenReturn(true);
            when(firebaseMessaging.send(message)).thenReturn("message-id");

            NotificationHistoryPayload payload = NotificationHistoryPayload.of(
                    traceId,
                    memberId,
                    NotificationHistoryType.FCM,
                    title,
                    body,
                    NotificationHistoryStatus.SUCCESS,
                    null
            );

            // when
            CompletableFuture<Void> result = fcmSendService.sendNotification(traceId, memberId, token, title, body);
            result.join();

            // then
            verify(notificationIdempotencyService).acquireProcessingKey(traceId);
            verify(firebaseMessaging).send(message);
            verify(fcmTokenService).updateLastUsedAt(token);
            verify(notificationHistoryService).createNotificationHistory(payload);
            verify(notificationIdempotencyService).markAsDone(traceId);
            verify(notificationIdempotencyService, never()).release(traceId);
            assertThat(result.isDone()).isTrue();
            assertThat(result.join()).isNull();
        }

        @Test
        @DisplayName("재시도 불가능한 예외 발생 시 토큰을 삭제하고 실패 히스토리를 저장한다")
        void sendNotification_fail_whenNonRetryableExceptionOccurs() throws FirebaseMessagingException {
            // given
            String traceId = "trace-id";
            Long memberId = 1L;
            String token = "failure-token";
            String title = "제목";
            String body = "본문";

            Message message = Message.builder().setToken(token).build();
            when(fcmMessageFactory.createMessage(token, title, body)).thenReturn(message);
            when(notificationIdempotencyService.acquireProcessingKey(traceId)).thenReturn(true);

            FirebaseMessagingException messagingException = mock(FirebaseMessagingException.class);
            when(messagingException.getMessage()).thenReturn("전송 실패");
            when(messagingException.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
            when(firebaseMessaging.send(message)).thenThrow(messagingException);

            NotificationHistoryPayload payload = NotificationHistoryPayload.of(
                    traceId,
                    memberId,
                    NotificationHistoryType.FCM,
                    title,
                    body,
                    NotificationHistoryStatus.FAILURE,
                    "전송 실패"
            );

            // when
            CompletableFuture<Void> result = fcmSendService.sendNotification(traceId, memberId, token, title, body);

            // then
            assertThat(result.isCompletedExceptionally()).isTrue();
            assertThatThrownBy(result::join).hasCauseInstanceOf(NonRetryableFcmException.class);
            verify(notificationIdempotencyService).acquireProcessingKey(traceId);
            verify(firebaseMessaging).send(message);
            verify(fcmTokenService).deleteToken(token);
            verify(fcmTokenService, never()).updateLastUsedAt(token);
            verify(notificationHistoryService).createNotificationHistory(payload);
            verify(notificationIdempotencyService).release(traceId);
            verify(notificationIdempotencyService, never()).markAsDone(traceId);
        }

        @Test
        @DisplayName("재시도 가능한 예외 발생 시 실패 히스토리를 저장하고 예외를 전달한다")
        void sendNotification_fail_whenRetryableExceptionOccurs() throws FirebaseMessagingException {
            // given
            String traceId = "trace-id";
            Long memberId = 1L;
            String token = "retry-token";
            String title = "제목";
            String body = "본문";

            Message message = Message.builder().setToken(token).build();
            when(fcmMessageFactory.createMessage(token, title, body)).thenReturn(message);
            when(notificationIdempotencyService.acquireProcessingKey(traceId)).thenReturn(true);

            FirebaseMessagingException messagingException = mock(FirebaseMessagingException.class);
            when(messagingException.getMessage()).thenReturn("일시적 오류");
            when(messagingException.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
            when(firebaseMessaging.send(message)).thenThrow(messagingException);

            NotificationHistoryPayload payload = NotificationHistoryPayload.of(
                    traceId,
                    memberId,
                    NotificationHistoryType.FCM,
                    title,
                    body,
                    NotificationHistoryStatus.FAILURE,
                    "일시적 오류"
            );

            // when
            CompletableFuture<Void> result = fcmSendService.sendNotification(traceId, memberId, token, title, body);

            // then
            assertThat(result.isCompletedExceptionally()).isTrue();
            assertThatThrownBy(result::join).hasCauseInstanceOf(RetryableFcmException.class);
            verify(notificationIdempotencyService).acquireProcessingKey(traceId);
            verify(firebaseMessaging).send(message);
            verify(notificationHistoryService).createNotificationHistory(payload);
            verify(notificationIdempotencyService).release(traceId);
            verify(notificationIdempotencyService, never()).markAsDone(traceId);
            verify(fcmTokenService, never()).deleteToken(token);
            verify(fcmTokenService, never()).updateLastUsedAt(token);
        }

        @Test
        @DisplayName("토큰이 비어 있으면 전송을 건너뛴다")
        void sendNotification_skip_whenTokenEmpty() {
            // given
            String traceId = "trace-id";
            Long memberId = 1L;
            String token = "";

            // when
            CompletableFuture<Void> result = fcmSendService.sendNotification(traceId, memberId, token, "제목", "본문");

            // then
            assertThat(result.isDone()).isTrue();
            assertThat(result.join()).isNull();
            verifyNoInteractions(fcmMessageFactory, fcmTokenService, notificationHistoryService, notificationIdempotencyService);
        }

        @Test
        @DisplayName("이미 처리 중인 요청이면 멱등성 키만 확인하고 전송을 중단한다")
        void sendNotification_skip_whenProcessingKeyExists() {
            // given
            String traceId = "trace-id";
            Long memberId = 1L;
            String token = "processing-token";

            when(notificationIdempotencyService.acquireProcessingKey(traceId)).thenReturn(false);

            // when
            CompletableFuture<Void> result = fcmSendService.sendNotification(traceId, memberId, token, "제목", "본문");
            result.join();

            // then
            verify(notificationIdempotencyService).acquireProcessingKey(traceId);
            verifyNoMoreInteractions(notificationIdempotencyService);
            verifyNoInteractions(fcmMessageFactory, firebaseMessaging, fcmTokenService, notificationHistoryService);
            assertThat(result.isDone()).isTrue();
        }
    }
}
