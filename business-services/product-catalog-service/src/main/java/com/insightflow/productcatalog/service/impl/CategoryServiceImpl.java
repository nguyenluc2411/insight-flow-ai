package com.insightflow.productcatalog.service.impl;

import com.insightflow.productcatalog.repository.ProductAliasRepository;
import com.insightflow.productcatalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String CAT_UNKNOWN = "CAT_UNKNOWN";

    private final ProductAliasRepository productAliasRepository;

    @Override
    public String resolveCategoryId(String keyword) {
        return productAliasRepository.findByKeywordIgnoreCase(keyword)
                .map(pa -> pa.getCategoryId())
                .orElse(CAT_UNKNOWN);
    }
}