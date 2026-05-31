package com.insightflow.notification.provider.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    @Override
    public void send(UUID recipientId, String subject, String body, String html) {
        String to = System.getenv("DEFAULT_NOTIFICATION_EMAIL");
        if (to == null || to.isEmpty()) {
            to = "huyhvse180722@fpt.edu.vn";
        }

        MimeMessage msg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setTo(to);
            helper.setSubject(subject);
            if (html != null && !html.isEmpty()) {
                helper.setText(html, true);
            } else {
                helper.setText(body, false);
            }
            mailSender.send(msg);
            log.info("SMTP email sent to={} subject={}", to, subject);
        } catch (MessagingException ex) {
            log.error("Failed to send SMTP email to={} error={}", to, ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}

