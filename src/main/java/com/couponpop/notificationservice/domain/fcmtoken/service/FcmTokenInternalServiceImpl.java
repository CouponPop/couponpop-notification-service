package com.couponpop.notificationservice.domain.fcmtoken.service;

import com.couponpop.couponpopcoremodule.dto.fcmtoken.request.FcmTokenExpireRequest;
import com.couponpop.couponpopcoremodule.dto.fcmtoken.response.FcmTokensResponse;
import com.couponpop.notificationservice.domain.fcmtoken.entity.FcmToken;
import com.couponpop.notificationservice.domain.fcmtoken.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FcmTokenInternalServiceImpl implements FcmTokenInternalService {

    private final FcmTokenRepository fcmTokenRepository;

    /**
     * TODO: Member Service의 로그아웃 API, 회원탈퇴 API 사용 필요
     * memberId는 authMember로 부터 획득
     * EDA 적용 가능
     */
    @Override
    public void expireFcmToken(FcmTokenExpireRequest fcmTokenExpireRequest, Long memberId) {

        // 기존 구현 메서드
        fcmTokenRepository
                .findByMemberIdAndFcmToken(memberId, fcmTokenExpireRequest.fcmToken())
                .ifPresent(fcmTokenRepository::delete);
    }

    @Override
    public List<FcmTokensResponse> fetchFcmTokensByMemberIds(List<Long> memberIds) {

        List<FcmToken> fcmTokens = fcmTokenRepository.findAllByMemberIdIn(memberIds);

        Map<Long, List<String>> tokensByMember = fcmTokens.stream()
                .collect(Collectors.groupingBy(
                        FcmToken::getMemberId,
                        Collectors.mapping(FcmToken::getFcmToken, Collectors.toList())
                ));

        return tokensByMember.entrySet().stream()
                .map(entry -> FcmTokensResponse.of(entry.getKey(), entry.getValue()))
                .toList();
    }
}
