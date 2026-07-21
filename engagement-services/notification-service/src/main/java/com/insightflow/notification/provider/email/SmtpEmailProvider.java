package com.insightflow.notification.provider.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    /** Địa chỉ From. Với Gmail nên trùng tài khoản đăng nhập (hoặc alias đã verify). */
    @Value("${app.notification.mail-from:${spring.mail.username:}}")
    private String mailFrom;

    /** Tên hiển thị người gửi trong hộp thư khách. */
    @Value("${app.invoice.company-name:Insight Flow AI}")
    private String fromName;

    @Override
    public void send(String recipientEmail, String subject, String body, String html) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email address cannot be null or empty");
        }

        log.info("[SMTP] START to={} subject={}", recipientEmail, subject);
        MimeMessage msg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) {
                try {
                    helper.setFrom(mailFrom, fromName);
                } catch (UnsupportedEncodingException enc) {
                    helper.setFrom(mailFrom);
                }
            }
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


