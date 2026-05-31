package com.insightflow.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.notification.mail-from:noreply@insightflow.ai}")
    private String mailFrom;

    /**
     * Sends a plain-text email asynchronously.
     * Fail-open: exceptions are logged but never propagated.
     */
    @Async
    public void sendAsync(String to, String subject, String text) {
        if (to == null || to.isBlank()) {
            log.debug("Email skipped — no recipient address");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("Email sent to={} subject={}", to, subject);
        } catch (MailException ex) {
            log.warn("Email delivery failed to={}: {}", to, ex.getMessage());
        }
    }
}
