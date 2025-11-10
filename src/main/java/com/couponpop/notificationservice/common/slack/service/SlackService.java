package com.couponpop.notificationservice.common.slack.service;

import com.slack.api.Slack;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.slack.api.webhook.WebhookPayloads.payload;

@Slf4j
@Service
public class SlackService {

    private final static String WARNING_COLOR = "#ffae42";

    private final Slack slackClient = Slack.getInstance();

    @Value("${webhook.slack.url}")
    private String webhookSlackUrl;

    public void sendMessage(String title, Map<String, String> data) {

        try {
            slackClient.send(webhookSlackUrl, payload(payloadBuilder ->
                    payloadBuilder
                            .text(title)
                            .attachments(generateSlackAttachments(data))
            ));
        } catch (IOException e) {
            log.debug("Slack 통신 중 예외 발생", e);
        }
    }

    private List<Attachment> generateSlackAttachments(Map<String, String> data) {
        List<Field> fields = data.entrySet().stream()
                .map(entry -> generateSlackField(entry.getKey(), entry.getValue()))
                .toList();

        Attachment attachment = Attachment.builder()
                .color(WARNING_COLOR)
                .fields(fields)
                .build();

        return List.of(attachment);
    }

    private Field generateSlackField(String title, String value) {
        return Field.builder()
                .title(title)
                .value(value)
                .valueShortEnough(false) // 길게 나오는 값도 잘리거나 하지 않도록 설정
                .build();
    }

}
