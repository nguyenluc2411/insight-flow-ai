package com.insightflow.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final Instant timestamp;
    private final boolean success;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(Instant.now(), true, message, data);
    }

    public static <T> ApiResponse<T> failure(String message, T data) {
        return new ApiResponse<>(Instant.now(), false, message, data);
    }
}
