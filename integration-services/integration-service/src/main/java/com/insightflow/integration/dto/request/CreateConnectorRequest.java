package com.insightflow.integration.dto.request;

import com.insightflow.integration.core.ConnectorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreateConnectorRequest {

    @NotNull(message = "connectorType is required")
    private ConnectorType connectorType;

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "credentials is required")
    private Map<String, String> credentials;

    private Map<String, Object> config;
}
