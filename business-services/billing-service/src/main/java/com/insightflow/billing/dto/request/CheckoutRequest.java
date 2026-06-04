package com.insightflow.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank(message = "Mã gói cước (packageCode) không được để trống")
    private String packageCode;

    @NotBlank(message = "Chu kỳ thanh toán (billingCycle) không được để trống - VD: MONTHLY, YEARLY")
    private String billingCycle;
}