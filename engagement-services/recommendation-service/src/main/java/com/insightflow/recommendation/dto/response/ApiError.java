package com.insightflow.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private String code;
    private String message;
    private Map<String, String> fields;
}

