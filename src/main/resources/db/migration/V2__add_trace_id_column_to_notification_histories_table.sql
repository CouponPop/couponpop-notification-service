-- trace_id 컬럼을 먼저 NULL 허용으로 추가
ALTER TABLE notification_histories
    ADD COLUMN trace_id VARCHAR(255) NULL;

-- 기존 데이터에 대해 trace_id 컬럼을 "-" 값으로 채우기
UPDATE notification_histories
SET trace_id = '-'
WHERE trace_id IS NULL;

-- 컬럼을 NOT NULL로 변경, UNIQUE 제약조건 추가
ALTER TABLE notification_histories
    MODIFY COLUMN trace_id VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '알림 발송 추적 및 멱등성 보장 ID',
    ADD UNIQUE KEY uk_notification_histories_trace_id (trace_id);