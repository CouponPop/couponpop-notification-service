package com.couponpop.notificationservice.common.config;


import com.couponpop.notificationservice.common.properties.AsyncFcmProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static com.couponpop.notificationservice.common.constants.AsyncExecutors.FCM_TASK_EXECUTOR;


@EnableAsync
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AsyncFcmProperties.class)
public class AsyncConfig {

    private final AsyncFcmProperties asyncFcmProperties;

    /**
     * FCM 비동기 작업 전용 스레드 풀.
     *
     * <ul>
     *   <li><strong>corePoolSize</strong>: 항상 유지할 스레드 수.</li>
     *   <li><strong>maxPoolSize</strong>: 트래픽 급증 시 확장 가능한 최대 스레드 수.</li>
     *   <li><strong>queueCapacity</strong>: 스레드가 모두 바쁠 때 대기열에서 버틸 수 있는 작업 수.</li>
     *   <li><strong>CallerRunsPolicy</strong>: 큐가 가득 차면 호출한 스레드가 직접 실행해 작업 유실을 방지.</li>
     *   <li><strong>waitForTasksToCompleteOnShutdown</strong>: 종료 시 진행 중인 작업을 안전하게 마무리.</li>
     * </ul>
     */
    @Bean(name = FCM_TASK_EXECUTOR)
    public Executor fcmTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncFcmProperties.getCorePoolSize());
        executor.setMaxPoolSize(asyncFcmProperties.getMaxPoolSize());
        executor.setQueueCapacity(asyncFcmProperties.getQueueCapacity());
        executor.setThreadNamePrefix(asyncFcmProperties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(asyncFcmProperties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(asyncFcmProperties.getAwaitTerminationSeconds());
        executor.initialize();
        return executor;
    }
}
