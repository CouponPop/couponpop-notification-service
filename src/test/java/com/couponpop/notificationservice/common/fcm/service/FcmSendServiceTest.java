package com.couponpop.notificationservice.common.fcm.service;

import com.couponpop.notificationservice.common.fcm.factory.FcmMessageFactory;
import com.couponpop.notificationservice.domain.fcmtoken.service.FcmTokenService;
import com.couponpop.notificationservice.domain.notificationhistory.dto.payload.NotificationHistoryPayload;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryStatus;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryType;
import com.couponpop.notificationservice.domain.notificationhistory.service.NotificationHistoryService;
import com.google.api.core.SettableApiFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmSendServiceTest {

    @Mock
    private FcmMessageFactory fcmMessageFactory;

    @Mock
    private NotificationHistoryService notificationHistoryService;

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
        void sendNotification_success() {
            // given
            Long memberId = 1L;
            String token = "success-token";
            String title = "제목";
            String body = "본문";

            Message message = Message.builder().setToken(token).build();
            when(fcmMessageFactory.createMessage(token, title, body)).thenReturn(message);

            SettableApiFuture<String> apiFuture = SettableApiFuture.create(); // sendAsync의 비동기 결과를 테스트에서 직접 제어한다.
            when(firebaseMessaging.sendAsync(message)).thenReturn(apiFuture);

            NotificationHistoryPayload payload = NotificationHistoryPayload.of(memberId, NotificationHistoryType.FCM, title, body, NotificationHistoryStatus.SUCCESS, null);

            // when
            CompletableFuture<Void> result = fcmSendService.sendNotification(memberId, token, title, body);
            apiFuture.set("message-id"); // 성공 콜백 트리거
            result.join(); // 비동기 작업이 완료될 때까지 대기

            // then
            verify(firebaseMessaging).sendAsync(message);
            verify(fcmTokenService).updateLastUsedAt(token);
            verify(notificationHistoryService).createNotificationHistory(payload);
            assertThat(result.isDone()).isTrue();
            assertThat(result.join()).isNull();
        }

        @Test
        @DisplayName("FCM 예외가 발생하면 토큰을 삭제하고 실패 히스토리를 저장한다")
        void sendNotification_failure() {
            // given
            Long memberId = 1L;
            String token = "failure-token";
            String title = "제목";
            String body = "본문";

            Message message = Message.builder().setToken(token).build();
            when(fcmMessageFactory.createMessage(token, title, body)).thenReturn(message);

            SettableApiFuture<String> apiFuture = SettableApiFuture.create();
            when(firebaseMessaging.sendAsync(message)).thenReturn(apiFuture);

            FirebaseMessagingException messagingException = mock(FirebaseMessagingException.class);
            when(messagingException.getMessage()).thenReturn("전송 실패");

            NotificationHistoryPayload payload = NotificationHistoryPayload.of(memberId, NotificationHistoryType.FCM, title, body, NotificationHistoryStatus.FAILURE, "전송 실패");

            // when
            CompletableFuture<Void> result = fcmSendService.sendNotification(memberId, token, title, body);
            // 실패 시나리오를 재현하기 위해 Future 예외를 수동으로 설정한다.
            // 실패 콜백을 실행해 토큰 삭제 및 히스토리 생성을 검증한다.
            apiFuture.setException(messagingException);

            // then
            assertThat(result.isCompletedExceptionally()).isTrue();
            verify(firebaseMessaging).sendAsync(message);
            verify(fcmTokenService).deleteToken(token);
            verify(notificationHistoryService).createNotificationHistory(payload);
        }

        @Test
        @DisplayName("토큰이 비어 있으면 전송을 건너뛴다")
        void sendNotification_skip_tokenEmpty() {
            // given
            Long memberId = 1L;
            String token = "";

            // when
            CompletableFuture<Void> result = fcmSendService.sendNotification(memberId, token, "제목", "본문");

            // then
            assertThat(result.isDone()).isTrue();
            assertThat(result.join()).isNull();
            verifyNoInteractions(fcmMessageFactory, fcmTokenService, notificationHistoryService);
        }
    }
}
