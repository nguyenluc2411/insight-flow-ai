package com.insightflow.notification.service.impl;

import com.insightflow.notification.dto.request.NotificationPreferenceRequest;
import com.insightflow.notification.dto.response.UserNotificationPreferenceResponse;
import com.insightflow.notification.entity.UserNotificationPreference;
import com.insightflow.notification.mapper.UserNotificationPreferenceMapper;
import com.insightflow.notification.repository.UserNotificationPreferenceRepository;
import com.insightflow.notification.service.interfaces.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final UserNotificationPreferenceMapper preferenceMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserNotificationPreferenceResponse> getPreferences(UUID userId) {
        return preferenceRepository.findByUserIdAndDeletedFalse(userId).stream()
                .map(preferenceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserNotificationPreferenceResponse upsert(UUID userId, NotificationPreferenceRequest request) {
        request.setUserId(userId);
        UserNotificationPreference entity = preferenceRepository
                .findByUserIdAndNotificationTypeAndChannel(userId, request.getNotificationType(), request.getChannel())
                .map(existing -> {
                    existing.setMinSeverity(request.getMinSeverity());
                    existing.setEnabled(request.isEnabled());
                    existing.setMuteUntil(request.getMuteUntil());
                    return existing;
                })
                .orElseGet(() -> preferenceMapper.toEntity(request));
        UserNotificationPreference saved = preferenceRepository.save(entity);
        return preferenceMapper.toResponse(saved);
    }
}
