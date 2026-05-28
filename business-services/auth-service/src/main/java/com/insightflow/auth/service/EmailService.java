package com.insightflow.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.reset-password.frontend-url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu Insight Flow");

            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            String html = """
                    <html>
                    <body>
                        <p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Insight Flow.</p>
                        <p>Nhấp vào liên kết bên dưới để đặt lại mật khẩu (liên kết có hiệu lực trong 1 giờ):</p>
                        <p><a href="%s">%s</a></p>
                        <p>Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>
                    </body>
                    </html>
                    """.formatted(resetLink, resetLink);

            helper.setText(html, true);

            mailSender.send(message);
            log.info("Password reset email sent to [REDACTED]");
        } catch (Exception ex) {
            log.error("Failed to send password reset email to [REDACTED]: {}", ex.getMessage());
        }
    }
}
