package com.insightflow.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public interface NotificationEvent {

    UUID eventId();

    String eventType();

    Instant timestamp();

    UUID correlationId();
}
