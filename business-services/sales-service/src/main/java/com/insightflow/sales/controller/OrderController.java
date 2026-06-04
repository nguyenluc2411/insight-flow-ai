package com.insightflow.sales.controller;

import com.insightflow.sales.dto.request.CreateOrderRequest;
import com.insightflow.sales.dto.response.SalesOrderResponse;
import com.insightflow.sales.service.OrderService;
import com.insightflow.security.CurrentUser;
import com.insightflow.security.RequiresPermission;
import com.insightflow.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales/orders")
@RequiredArgsConstructor
@Tag(name = "Sales Orders", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @RequiresPermission("sales:read")
    @Operation(summary = "List orders", description = "Paginated order list for the tenant")
    @ApiResponse(responseCode = "200", description = "Success")
    public Page<SalesOrderResponse> listOrders(
            @CurrentUser UserContext user,
            @PageableDefault(size = 20) Pageable pageable) {
        return orderService.getOrders(user.tenantId(), pageable);
    }

    @PostMapping
    @RequiresPermission("sales:write")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create order", description = "Creates a new order in pending status")
    @ApiResponse(responseCode = "201", description = "Order created")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public SalesOrderResponse createOrder(
            @CurrentUser UserContext user,
            @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request, user.tenantId());
    }

    @GetMapping("/{id}")
    @RequiresPermission("sales:read")
    @Operation(summary = "Get order by ID")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Not found")
    public SalesOrderResponse getOrder(
            @CurrentUser UserContext user,
            @PathVariable UUID id) {
        return orderService.getOrderById(id, user.tenantId());
    }

    @PostMapping("/{id}/complete")
    @RequiresPermission("sales:write")
    @Operation(summary = "Complete order",
               description = "Transitions order from pending to completed. Publishes sales.order.completed to Kafka.")
    @ApiResponse(responseCode = "200", description = "Order completed")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @ApiResponse(responseCode = "422", description = "Order already completed or cancelled")
    public SalesOrderResponse completeOrder(
            @CurrentUser UserContext user,
            @PathVariable UUID id) {
        return orderService.completeOrder(id, user.tenantId());
    }
}
