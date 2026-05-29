package com.insightflow.notification.provider.email;

import java.util.UUID;

public interface EmailProvider {

    void send(UUID recipientId, String subject, String body, String html);
}
