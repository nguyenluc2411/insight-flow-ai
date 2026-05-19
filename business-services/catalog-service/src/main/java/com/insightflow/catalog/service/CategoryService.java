package com.insightflow.catalog.service;

import com.insightflow.catalog.dto.response.CategorySummaryItem;
import com.insightflow.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategorySummaryItem> getCategories(UUID tenantId) {
        return categoryRepository.findSummariesByTenantId(tenantId);
    }
}
