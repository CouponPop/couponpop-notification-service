package com.couponpop.notificationservice.domain.fcmtoken.service;

import com.couponpop.couponpopcoremodule.dto.fcmtoken.request.FcmTokenExpireRequest;
import com.couponpop.couponpopcoremodule.dto.fcmtoken.response.FcmTokensResponse;

import java.util.List;

public interface FcmTokenInternalService {

    void expireFcmToken(FcmTokenExpireRequest fcmTokenExpireRequest, Long memberId);

    List<FcmTokensResponse> fetchFcmTokensByMemberIds(List<Long> memberIds);

    FcmTokensResponse fetchFcmTokensByMemberId(Long memberId);
}
