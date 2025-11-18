package com.couponpop.notificationservice.domain.notificationhistory.service;

import com.couponpop.notificationservice.domain.notificationhistory.dto.payload.NotificationHistoryPayload;
import com.couponpop.notificationservice.domain.notificationhistory.entity.NotificationHistory;
import com.couponpop.notificationservice.domain.notificationhistory.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotificationHistory(NotificationHistoryPayload payload) {
        NotificationHistory notificationHistory = NotificationHistory.of(payload.traceId(), payload.memberId(), payload.type(), payload.title(), payload.body(), payload.status(), payload.failureReason());
        notificationHistoryRepository.save(notificationHistory);
    }
}
