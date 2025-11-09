-- trace_id 유니크키 제거
ALTER TABLE notification_histories
    DROP INDEX uk_notification_histories_trace_id;
