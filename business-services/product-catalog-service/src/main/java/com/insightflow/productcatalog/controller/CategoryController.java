package com.insightflow.productcatalog.controller;

import com.insightflow.productcatalog.dto.response.ApiResponse;
import com.insightflow.productcatalog.dto.response.ResolveCategoryResponse;
import com.insightflow.productcatalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<ResolveCategoryResponse>> resolve(@RequestParam("keyword") String keyword) {
        String categoryId = categoryService.resolveCategoryId(keyword);
        ResolveCategoryResponse response = ResolveCategoryResponse.builder()
                .categoryId(categoryId)
                .build();
        return ResponseEntity.ok(ApiResponse.<ResolveCategoryResponse>builder()
                .success(true)
                .message("OK")
                .data(response)
                .errorCode(null)
                .timestamp(OffsetDateTime.now().toString())
                .build());
    }
}