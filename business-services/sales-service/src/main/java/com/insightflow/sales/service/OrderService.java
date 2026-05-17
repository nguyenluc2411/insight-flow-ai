package com.insightflow.sales.service;

import com.insightflow.sales.dto.request.CreateOrderRequest;
import com.insightflow.sales.dto.request.OrderItemRequest;
import com.insightflow.sales.dto.response.SalesOrderResponse;
import com.insightflow.sales.entity.Customer;
import com.insightflow.sales.entity.SalesOrder;
import com.insightflow.sales.entity.SalesOrderItem;
import com.insightflow.sales.event.OrderCompletedEvent;
import com.insightflow.sales.exception.OrderStateException;
import com.insightflow.sales.exception.ResourceNotFoundException;
import com.insightflow.sales.mapper.SalesOrderMapper;
import com.insightflow.sales.repository.CustomerRepository;
import com.insightflow.sales.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final SalesOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final SalesOrderMapper orderMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public SalesOrderResponse createOrder(CreateOrderRequest request, UUID tenantId) {
        SalesOrder order = new SalesOrder();
        order.setTenantId(tenantId);
        order.setOrderNumber(generateOrderNumber(tenantId));
        order.setLocationId(request.getLocationId());
        order.setChannel(request.getChannel() != null ? request.getChannel() : "pos");
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("pending");

        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findByTenantIdAndId(tenantId, request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));
            order.setCustomer(customer);
        }

        // Build items and compute totals
        List<SalesOrderItem> items = request.getItems().stream()
                .map(ir -> buildItem(ir, order, tenantId))
                .toList();
        order.getItems().addAll(items);

        BigDecimal subtotal = items.stream()
                .map(SalesOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal);

        SalesOrder saved = orderRepository.save(order);
        log.debug("Created order id={} number={} tenantId={}", saved.getId(), saved.getOrderNumber(), tenantId);
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public SalesOrderResponse completeOrder(UUID orderId, UUID tenantId) {
        SalesOrder order = orderRepository.findByTenantIdAndId(tenantId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", orderId));

        if (!"pending".equals(order.getStatus())) {
            throw new OrderStateException("Order " + orderId + " is already " + order.getStatus());
        }

        order.setStatus("completed");
        SalesOrder saved = orderRepository.save(order);

        // Update customer stats
        if (order.getCustomer() != null) {
            updateCustomerStats(order.getCustomer(), order.getTotalAmount());
        }

        // Publish event — fail-open
        publishOrderCompleted(saved);

        log.info("Completed order id={} tenantId={}", orderId, tenantId);
        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SalesOrderResponse> getOrders(UUID tenantId, Pageable pageable) {
        return orderRepository.findByTenantId(tenantId, pageable)
                .map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse getOrderById(UUID orderId, UUID tenantId) {
        return orderRepository.findByTenantIdAndId(tenantId, orderId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", orderId));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private SalesOrderItem buildItem(OrderItemRequest ir, SalesOrder order, UUID tenantId) {
        SalesOrderItem item = new SalesOrderItem();
        item.setTenantId(tenantId);
        item.setOrder(order);
        item.setVariantId(ir.getVariantId());
        item.setQuantity(ir.getQuantity());
        item.setUnitPrice(ir.getUnitPrice());
        item.setUnitCost(ir.getUnitCost());

        BigDecimal discount = ir.getDiscountAmount() != null ? ir.getDiscountAmount() : BigDecimal.ZERO;
        item.setDiscountAmount(discount);

        BigDecimal lineTotal = ir.getUnitPrice()
                .multiply(BigDecimal.valueOf(ir.getQuantity()))
                .subtract(discount);
        item.setLineTotal(lineTotal);
        return item;
    }

    private void updateCustomerStats(Customer customer, BigDecimal orderTotal) {
        customer.setTotalSpent(customer.getTotalSpent().add(orderTotal));
        customer.setOrderCount(customer.getOrderCount() + 1);
        customer.setLastOrderAt(Instant.now());
        customerRepository.save(customer);
    }

    private void publishOrderCompleted(SalesOrder order) {
        List<OrderCompletedEvent.OrderItem> eventItems = order.getItems().stream()
                .map(i -> OrderCompletedEvent.OrderItem.builder()
                        .variantId(i.getVariantId())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .toList();

        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(OrderCompletedEvent.TYPE)
                .tenantId(order.getTenantId())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .locationId(order.getLocationId())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .channel(order.getChannel())
                .totalAmount(order.getTotalAmount())
                .items(eventItems)
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(OrderCompletedEvent.TOPIC, order.getTenantId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCompletedEvent orderId={}: {}",
                                order.getId(), ex.getMessage());
                    } else {
                        log.debug("Published OrderCompletedEvent orderId={} offset={}",
                                order.getId(), result.getRecordMetadata().offset());
                    }
                });
    }

    private String generateOrderNumber(UUID tenantId) {
        // Format: ORD-<first8charsOfTenantId>-<epochMillis>
        String tenantPrefix = tenantId.toString().substring(0, 8).toUpperCase();
        return "ORD-" + tenantPrefix + "-" + System.currentTimeMillis();
    }
}
