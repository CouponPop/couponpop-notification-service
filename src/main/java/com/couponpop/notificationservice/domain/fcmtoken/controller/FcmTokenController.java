package com.couponpop.notificationservice.domain.fcmtoken.controller;

import com.couponpop.notificationservice.common.response.ApiResponse;
import com.couponpop.notificationservice.domain.fcmtoken.dto.request.FcmTokenRequest;
import com.couponpop.notificationservice.domain.fcmtoken.service.FcmTokenService;
import com.couponpop.security.annotation.CurrentMember;
import com.couponpop.security.dto.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> upsertFcmToken(@RequestBody @Valid FcmTokenRequest request, @CurrentMember AuthMember authMember) {
        fcmTokenService.upsertTokenForMember(request, authMember.id());
        return ApiResponse.noContent();
    }

}
