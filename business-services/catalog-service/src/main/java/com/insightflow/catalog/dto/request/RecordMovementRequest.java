package com.insightflow.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class RecordMovementRequest {

    @NotNull
    private UUID variantId;

    @NotNull
    private UUID locationId;

    /**
     * movement_type values: PURCHASE, SALE, RETURN, ADJUSTMENT, TRANSFER_IN, TRANSFER_OUT
     */
    @NotBlank
    @Size(max = 30)
    private String movementType;

    /** Positive = stock in, negative = stock out */
    @NotNull
    private Integer quantityChange;

    @Size(max = 50)
    private String referenceType;

    private UUID referenceId;

    private String notes;
}
