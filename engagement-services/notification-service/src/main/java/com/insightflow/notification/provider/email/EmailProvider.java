package com.insightflow.notification.provider.email;

public interface EmailProvider {

    void send(String recipientEmail, String subject, String body, String html);
}

