package com.insightflow.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String location;
    private List<String> categories;
    private String businessScale;
    private List<String> platforms;
    private Boolean profileComplete;
}
