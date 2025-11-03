package com.couponpop.notificationservice.domain.fcmtoken.entity;

import com.couponpop.notificationservice.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String fcmToken;

    @Column(nullable = false, length = 32)
    private String deviceType;

    @Column(nullable = false, length = 128)
    private String deviceIdentifier;

    @Column(nullable = false)
    private boolean notificationEnabled;

    private LocalDateTime lastUsedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private FcmToken(Long memberId,
                     String fcmToken,
                     String deviceType,
                     String deviceIdentifier,
                     boolean notificationEnabled,
                     LocalDateTime lastUsedAt) {
        this.memberId = memberId;
        this.fcmToken = fcmToken;
        this.deviceType = deviceType;
        this.deviceIdentifier = deviceIdentifier;
        this.notificationEnabled = notificationEnabled;
        this.lastUsedAt = lastUsedAt;
    }

    public static FcmToken of(Long memberId,
                              String fcmToken,
                              String deviceType,
                              String deviceIdentifier,
                              boolean notificationEnabled,
                              LocalDateTime lastUsedAt) {
        return FcmToken.builder()
                .memberId(memberId)
                .fcmToken(fcmToken)
                .deviceType(deviceType)
                .deviceIdentifier(deviceIdentifier)
                .notificationEnabled(notificationEnabled)
                .lastUsedAt(lastUsedAt)
                .build();
    }

    public void updateFcmToken(String fcmToken, LocalDateTime lastUsedAt) {
        this.fcmToken = fcmToken;
        this.lastUsedAt = lastUsedAt;
    }

    public void updateMemberIdAndDeviceIdentifier(Long memberId, String deviceIdentifier, LocalDateTime lastUsedAt) {
        this.memberId = memberId;
        this.deviceIdentifier = deviceIdentifier;
        this.lastUsedAt = lastUsedAt;
    }

    public void updateLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

}
