package com.insightflow.userworkspace.exception;

import com.insightflow.userworkspace.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .errorCode(ex.getErrorCode())
                .timestamp(OffsetDateTime.now().toString())
                .build();
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Lỗi máy chủ nội bộ hệ thống")
                .data(null)
                .errorCode("INTERNAL_ERROR")
                .timestamp(OffsetDateTime.now().toString())
                .build();
        return ResponseEntity.ok(response);
    }
}