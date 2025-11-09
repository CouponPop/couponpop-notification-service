package com.couponpop.notificationservice.domain.notificationhistory.entity;

import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryStatus;
import com.couponpop.notificationservice.domain.notificationhistory.enums.NotificationHistoryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String traceId;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationHistoryType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationHistoryStatus status;

    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private NotificationHistory(Long memberId, String traceId, NotificationHistoryType type, String title, String body, NotificationHistoryStatus status, String failureReason) {
        this.memberId = memberId;
        this.traceId = traceId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.status = status;
        this.failureReason = failureReason;
    }

    public static NotificationHistory of(Long memberId, String traceId, NotificationHistoryType type, String title, String body, NotificationHistoryStatus status, String failureReason) {
        return NotificationHistory.builder()
                .memberId(memberId)
                .traceId(traceId)
                .type(type)
                .title(title)
                .body(body)
                .status(status)
                .failureReason(failureReason)
                .build();
    }

}
