package com.insightflow.integration.service;

import com.insightflow.integration.dto.ImportResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Parses an uploaded CSV/XLSX sales file and emits the same domain events as a POS
 * sync ({@code integration.product.synced} then {@code integration.order.synced}),
 * so file-uploaded data flows through the identical catalog → ml pipeline.
 */
public interface ImportService {

    /**
     * @param tenantId owning tenant (from {@code @CurrentUser})
     * @param file     uploaded CSV/XLSX/XLS file
     * @return summary with row/product/order counts and a status the FE can act on
     */
    ImportResultDto importFile(UUID tenantId, MultipartFile file);
}
