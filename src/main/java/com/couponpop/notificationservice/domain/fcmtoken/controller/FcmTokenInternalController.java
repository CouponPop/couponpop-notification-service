package com.couponpop.notificationservice.domain.fcmtoken.controller;

import com.couponpop.couponpopcoremodule.dto.fcmtoken.request.FcmTokenExpireRequest;
import com.couponpop.couponpopcoremodule.dto.fcmtoken.response.FcmTokensResponse;
import com.couponpop.notificationservice.common.response.ApiResponse;
import com.couponpop.notificationservice.domain.fcmtoken.service.FcmTokenInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class FcmTokenInternalController {

    private final FcmTokenInternalService fcmTokenInternalService;

    @PostMapping("/v1/fcm-token/expire")
    public ResponseEntity<ApiResponse<Void>> expireFcmToken(@RequestBody FcmTokenExpireRequest fcmTokenExpireRequest) {

        fcmTokenInternalService.expireFcmToken(fcmTokenExpireRequest);
        return ApiResponse.noContent();
    }

    @PostMapping("/v1/fcm-tokens/search")
    public ResponseEntity<ApiResponse<List<FcmTokensResponse>>> fetchFcmTokensByMemberIds(@RequestBody List<Long> memberIds) {

        List<FcmTokensResponse> response = fcmTokenInternalService.fetchFcmTokensByMemberIds(memberIds);
        return ApiResponse.success(response);
    }

    @GetMapping("/v1/fcm-tokens/members/{memberId}")
    public ResponseEntity<ApiResponse<FcmTokensResponse>> fetchFcmTokensByMemberId(@PathVariable Long memberId) {

        FcmTokensResponse response = fcmTokenInternalService.fetchFcmTokensByMemberId(memberId);
        return ApiResponse.success(response);
    }
}
