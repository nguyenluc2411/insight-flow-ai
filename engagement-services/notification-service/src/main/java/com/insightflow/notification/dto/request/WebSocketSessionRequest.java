package com.insightflow.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketSessionRequest {

    @NotBlank
    @Size(max = 128)
    private String sessionId;

    @NotNull
    private UUID userId;

    @Size(max = 100)
    private String nodeId;

    @Size(max = 100)
    private String clientId;

    @Size(max = 45)
    private String ipAddress;

    @Size(max = 255)
    private String userAgent;
}
