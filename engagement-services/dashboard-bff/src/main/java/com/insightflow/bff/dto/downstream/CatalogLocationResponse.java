package com.insightflow.bff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogLocationResponse {
    private UUID id;
    private String name;
    private String city;
    private boolean active;
}
