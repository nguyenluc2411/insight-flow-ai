package com.insightflow.notification.service;

import com.insightflow.notification.dto.request.UpsertPreferenceRequest;
import com.insightflow.notification.dto.response.PreferenceResponse;
import com.insightflow.notification.entity.NotificationPreference;
import com.insightflow.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public List<PreferenceResponse> getPreferences(UUID tenantId) {
        return preferenceRepository.findByTenantId(tenantId).stream()
                .map(PreferenceResponse::from)
                .toList();
    }

    @Transactional
    public PreferenceResponse upsert(UUID tenantId, UUID userId, UpsertPreferenceRequest request) {
        NotificationPreference pref = preferenceRepository
                .findByTenantIdAndEventTypeAndChannel(tenantId, request.getEventType(), request.getChannel())
                .orElseGet(() -> {
                    NotificationPreference np = new NotificationPreference();
                    np.setTenantId(tenantId);
                    np.setUserId(userId);
                    np.setEventType(request.getEventType());
                    np.setChannel(request.getChannel());
                    return np;
                });

        pref.setEnabled(request.isEnabled());
        if (request.getThreshold() != null) {
            pref.setThreshold(request.getThreshold());
        }
        return PreferenceResponse.from(preferenceRepository.save(pref));
    }
}
