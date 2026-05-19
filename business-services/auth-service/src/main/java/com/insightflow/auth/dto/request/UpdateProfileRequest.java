package com.insightflow.auth.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {
    private String location;
    private List<String> categories;
    private String businessScale;
    private List<String> platforms;
    private Boolean profileComplete;
}
