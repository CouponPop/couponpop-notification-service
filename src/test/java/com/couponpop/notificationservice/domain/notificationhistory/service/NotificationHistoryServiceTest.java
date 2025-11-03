package com.couponpop.notificationservice.domain.notificationhistory.service;

import com.couponpop.notificationservice.domain.notificationhistory.dto.payload.NotificationHistoryPayload;
import com.couponpop.notificationservice.domain.notificationhistory.entity.NotificationHistory;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryStatus;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryType;
import com.couponpop.notificationservice.domain.notificationhistory.repository.NotificationHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @InjectMocks
    private NotificationHistoryService notificationHistoryService;

    @Nested
    @DisplayName("알림 히스토리 생성")
    class CreateNotificationHistory {

        @Test
        @DisplayName("성공 상태의 알림 히스토리를 저장한다")
        void createNotificationHistory_success_statusSuccess() {
            // given
            Long memberId = 1L;
            NotificationHistoryPayload payload = NotificationHistoryPayload.of(memberId, NotificationHistoryType.FCM, "제목", "본문", NotificationHistoryStatus.SUCCESS, null);

            // when
            notificationHistoryService.createNotificationHistory(payload);

            // then
            ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
            verify(notificationHistoryRepository).save(captor.capture());

            NotificationHistory saved = captor.getValue();
            assertThat(saved.getMemberId()).isEqualTo(memberId);
            assertThat(saved.getType()).isEqualTo(NotificationHistoryType.FCM);
            assertThat(saved.getTitle()).isEqualTo("제목");
            assertThat(saved.getBody()).isEqualTo("본문");
            assertThat(saved.getStatus()).isEqualTo(NotificationHistoryStatus.SUCCESS);
            assertThat(saved.getFailureReason()).isNull();
            verifyNoMoreInteractions(notificationHistoryRepository);
        }

        @Test
        @DisplayName("실패 상태와 실패 사유를 함께 저장한다")
        void createNotificationHistory_success_statusFailure() {
            // given
            Long memberId = 2L;
            NotificationHistoryPayload payload = NotificationHistoryPayload.of(memberId, NotificationHistoryType.FCM, "실패 제목", "실패 본문", NotificationHistoryStatus.FAILURE, "전송 실패");

            // when
            notificationHistoryService.createNotificationHistory(payload);

            // then
            ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
            verify(notificationHistoryRepository).save(captor.capture());

            NotificationHistory saved = captor.getValue();
            assertThat(saved.getMemberId()).isEqualTo(memberId);
            assertThat(saved.getType()).isEqualTo(NotificationHistoryType.FCM);
            assertThat(saved.getTitle()).isEqualTo("실패 제목");
            assertThat(saved.getBody()).isEqualTo("실패 본문");
            assertThat(saved.getStatus()).isEqualTo(NotificationHistoryStatus.FAILURE);
            assertThat(saved.getFailureReason()).isEqualTo("전송 실패");
            verifyNoMoreInteractions(notificationHistoryRepository);
        }
    }
}
