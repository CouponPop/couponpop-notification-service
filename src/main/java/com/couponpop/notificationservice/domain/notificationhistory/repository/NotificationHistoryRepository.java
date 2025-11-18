package com.couponpop.notificationservice.domain.notificationhistory.repository;

import com.couponpop.notificationservice.domain.notificationhistory.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
}
