package com.insightflow.notification.service.template;

import com.insightflow.notification.entity.NotificationTemplate;

import java.util.Map;
import java.util.Optional;

public interface NotificationTemplateService {

    Optional<NotificationTemplate> findActiveTemplate(String templateKey);

    String renderTemplateBody(NotificationTemplate template, Map<String, Object> model);

    String renderTemplateHtml(NotificationTemplate template, Map<String, Object> model);
}
