package com.insightflow.notification.dto.request;

import com.insightflow.notification.enums.InboxStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInboxStatusRequest {

    @NotNull
    private InboxStatus inboxStatus;
}
