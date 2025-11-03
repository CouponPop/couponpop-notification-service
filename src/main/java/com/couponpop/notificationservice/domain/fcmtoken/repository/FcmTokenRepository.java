package com.couponpop.notificationservice.domain.fcmtoken.repository;

import com.couponpop.notificationservice.domain.fcmtoken.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByMemberIdAndDeviceIdentifier(Long memberId, String deviceIdentifier);

    Optional<FcmToken> findByFcmToken(String fcmToken);

    List<FcmToken> findByMemberIdAndNotificationEnabledIsTrue(Long memberId);

    Optional<FcmToken> findByMemberIdAndFcmToken(Long memberId, String fcmToken);
}
