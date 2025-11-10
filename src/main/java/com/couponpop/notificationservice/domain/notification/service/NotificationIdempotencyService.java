package com.couponpop.notificationservice.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class NotificationIdempotencyService {

    private static final Duration PROCESSION_TTL = Duration.ofMinutes(5);
    private static final Duration DONE_TTL = Duration.ofDays(7);
    private static final String KEY_PATTERN = "notification:idempotency:%s";
    private static final String PROCESSING_VALUE = "processing";
    private static final String DONE_VALUE = "done";

    private final StringRedisTemplate stringRedisTemplate;

    public boolean acquireProcessingKey(String traceId) {

        String key = KEY_PATTERN.formatted(traceId);
        // 키가 없을 때만 설정 (멱등성 보장)
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, PROCESSING_VALUE, PROCESSION_TTL);

        return Boolean.TRUE.equals(success);
    }

    public void markAsDone(String traceId) {

        String key = KEY_PATTERN.formatted(traceId);
        stringRedisTemplate.opsForValue().set(key, DONE_VALUE, DONE_TTL);
    }

    public void release(String traceId) {

        stringRedisTemplate.delete(KEY_PATTERN.formatted(traceId));
    }
}
