package com.insightflow.integration.dto.request;

import com.insightflow.integration.core.SyncType;
import lombok.Data;

@Data
public class TriggerSyncRequest {

    private SyncType syncType = SyncType.FULL_RECONCILIATION;
}
