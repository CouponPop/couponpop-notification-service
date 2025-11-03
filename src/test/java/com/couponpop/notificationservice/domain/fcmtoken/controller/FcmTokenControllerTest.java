package com.couponpop.notificationservice.domain.fcmtoken.controller;

import com.couponpop.notificationservice.domain.fcmtoken.dto.request.FcmTokenRequest;
import com.couponpop.notificationservice.domain.fcmtoken.service.FcmTokenService;
import com.couponpop.security.dto.AuthMember;
import com.couponpop.security.token.JwtAuthFilter;
import com.couponpop.security.token.JwtAuthenticationToken;
import com.couponpop.security.token.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO: 추후 Swagger & REST Docs 통합 작업 시 주석 해제
//@AutoConfigureRestDocs
@WebMvcTest(FcmTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
class FcmTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private FcmTokenService fcmTokenService;

    @BeforeEach
    void setUp() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        AuthMember authMember = AuthMember.of(123L, "testUser", "CUSTOMER");
        Authentication authenticationToken = new JwtAuthenticationToken(authMember);
        context.setAuthentication(authenticationToken);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/fcm-tokens")
    class UpsertFcmToken {

        private static final String URL = "/api/v1/fcm-token";

        @Test
        @DisplayName("FCM 토큰 생성 및 갱신 - 성공")
        void upsertFcmToken_success() throws Exception {
            // given
            FcmTokenRequest request = FcmTokenRequest.builder()
                    .fcmToken("sample_fcm_token")
                    .deviceType("ANDROID")
                    .deviceIdentifier("device-123")
                    .build();

            willDoNothing().given(fcmTokenService).upsertTokenForMember(any(FcmTokenRequest.class), anyLong());

            // when
            ResultActions resultActions = mockMvc.perform(
                    post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andDo(print())
                    .andExpect(status().isNoContent());

            // docs
            // TODO: 추후 Swagger & REST Docs 통합 작업 시 주석 해제 및 문서화 진행
//            resultActions.andDo(document("fcmtoken-fcmTokenUpsert",
//                    preprocessRequest(prettyPrint()),
//                    preprocessResponse(prettyPrint()),
//                    resource(
//                            ResourceSnippetParameters.builder()
//                                    .summary("FCM 토큰 생성 및 갱신")
//                                    .description("회원의 FCM 토큰을 생성하거나 갱신합니다.")
//                                    .tag("FCM Token")
//                                    .requestSchema(Schema.schema("FcmToken.fcmTokenRequest"))
//                                    .requestFields(
//                                            fieldWithPath("fcmToken").description("FCM 토큰"),
//                                            fieldWithPath("deviceType").description("디바이스 타입 (예: ANDROID, IOS)"),
//                                            fieldWithPath("deviceIdentifier").description("디바이스 고유 식별자")
//                                    )
//                                    .build()
//                    )));
        }
    }

}