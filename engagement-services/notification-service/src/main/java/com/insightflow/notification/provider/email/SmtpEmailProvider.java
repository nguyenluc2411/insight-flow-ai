package com.insightflow.notification.provider.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    @Override
    public void send(String recipientEmail, String subject, String body, String html) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email address cannot be null or empty");
        }

        log.info("[SMTP] START to={} subject={}", recipientEmail, subject);
        MimeMessage msg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            if (html != null && !html.isEmpty()) {
                helper.setText(html, true);
            } else {
                helper.setText(body, false);
            }
            mailSender.send(msg);
            log.info("[SMTP] SENT to={} subject={}", recipientEmail, subject);
        } catch (MessagingException | MailException ex) {
            log.error("[SMTP] FAILED to={} error={}", recipientEmail, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}


