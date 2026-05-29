package com.insightflow.integration.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConnectorRegistry {

    private final Map<ConnectorType, ConnectorInterface> connectors;

    public ConnectorRegistry(List<ConnectorInterface> connectorList) {
        this.connectors = connectorList.stream()
                .collect(Collectors.toMap(ConnectorInterface::getType, Function.identity()));
        log.info("ConnectorRegistry initialized with {} connector(s): {}",
                connectors.size(), connectors.keySet());
    }

    public ConnectorInterface get(ConnectorType type) {
        ConnectorInterface connector = connectors.get(type);
        if (connector == null) {
            throw new IllegalArgumentException("No connector registered for type: " + type);
        }
        return connector;
    }

    public Map<ConnectorType, ConnectorInterface> getAll() {
        return Map.copyOf(connectors);
    }
}
