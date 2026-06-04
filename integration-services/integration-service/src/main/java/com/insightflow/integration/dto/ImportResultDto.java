package com.insightflow.integration.dto;

/**
 * Result of a file import. Returned to the frontend, which reads {@code status}
 * ("completed" → success, "processing" → poll) plus {@code fileId}/{@code message}.
 * The row counts give the user immediate feedback on how much was ingested.
 */
public record ImportResultDto(
        String fileId,
        String fileName,
        String status,
        int totalRows,
        int validRows,
        int skippedRows,
        int productCount,
        int orderCount,
        String message
) {
}
