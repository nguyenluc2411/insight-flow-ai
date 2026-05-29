package com.insightflow.notification.service.template;

import com.insightflow.notification.entity.NotificationTemplate;
import com.insightflow.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final TemplateEngine templateEngine;

    @Override
    public Optional<NotificationTemplate> findActiveTemplate(String templateKey) {
        return templateRepository.findByTemplateKeyAndChannelAndActiveTrue(templateKey, com.insightflow.notification.enums.NotificationChannel.EMAIL)
                .map(t -> t);
    }

    @Override
    public String renderTemplateBody(NotificationTemplate template, Map<String, Object> model) {
        if (template == null) return null;
        if (template.getBody() == null) return null;
        Context ctx = new Context();
        if (model != null) ctx.setVariables(model);
        return templateEngine.process(template.getBody(), ctx);
    }

    @Override
    public String renderTemplateHtml(NotificationTemplate template, Map<String, Object> model) {
        if (template == null) return null;
        if (template.getHtmlBody() == null) return null;
        Context ctx = new Context();
        if (model != null) ctx.setVariables(model);
        return templateEngine.process(template.getHtmlBody(), ctx);
    }
}
