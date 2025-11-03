package com.couponpop.notificationservice.common.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FCM 비동기 작업용 스레드풀 설정 프로퍼티.
 * <p>환경별로 풀 크기와 스레드 이름 등을 조정하기 위해 {@code async.fcm} 접두어를 사용한다</p>
 */
@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "async.fcm")
public class AsyncFcmProperties {

    // 기본 스레드 수
    private int corePoolSize;

    // 최대 스레드 수
    private int maxPoolSize;

    // 대기열 용량
    private int queueCapacity;

    // 스레드 이름 접두어
    private String threadNamePrefix;

    // 애플리케이션 종료 시 작업 완료 대기 여부
    private boolean waitForTasksToCompleteOnShutdown;

    // 종료 대기 시간(초)
    private int awaitTerminationSeconds;
}

