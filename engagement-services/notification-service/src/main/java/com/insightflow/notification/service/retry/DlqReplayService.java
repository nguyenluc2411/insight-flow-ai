package com.insightflow.notification.service.retry;

import com.insightflow.notification.dto.response.DlqReplayResponse;

import java.util.UUID;

public interface DlqReplayService {

    DlqReplayResponse replay(UUID notificationId, boolean admin);
}

