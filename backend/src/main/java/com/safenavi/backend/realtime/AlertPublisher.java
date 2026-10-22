package com.safenavi.backend.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlertPublisher {

    public static final String TOPIC_PREFIX = "/topic/alerts/";

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public AlertPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Fire-and-forget: a failed broadcast must not fail the report that triggered it. */
    public void publish(String regionId, ReportAlert alert) {
        try {
            messagingTemplate.convertAndSend(TOPIC_PREFIX + regionId, alert);
        } catch (MessagingException e) {
            log.warn("Failed to publish alert for report {} to region {}: {}", alert.reportId(), regionId, e.getMessage());
        }
    }
}
