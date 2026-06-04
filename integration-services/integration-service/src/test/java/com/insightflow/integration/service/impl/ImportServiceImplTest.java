package com.insightflow.integration.service.impl;

import com.insightflow.common.events.integration.OrderSyncedEvent;
import com.insightflow.common.events.integration.ProductSyncedEvent;
import com.insightflow.common.fileparse.DynamicFileParser;
import com.insightflow.common.web.exception.BusinessException;
import com.insightflow.common.web.exception.ValidationException;
import com.insightflow.integration.dto.ImportResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportServiceImplTest {

    private static final String PRODUCT_TOPIC = "integration.product.synced";
    private static final String ORDER_TOPIC = "integration.order.synced";
    private static final UUID TENANT = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private ImportServiceImpl service;

    @BeforeEach
    void setUp() {
        // Real parser (plain class), mocked Kafka. send() returns an un-completed
        // future so the whenComplete callback simply never fires.
        service = new ImportServiceImpl(new DynamicFileParser(), kafkaTemplate);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<SendResult<String, Object>>());
    }

    private MultipartFile csv(String name, String body) {
        return new MockMultipartFile("file", name, "text/csv", body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validCsv_emitsProductThenOrder() {
        String body = """
                sku,ten_san_pham,so_luong_da_ban,ngay_giao_dich,gia_ban,danh_muc
                AO001,Áo thun trắng,5,2026-05-01,150000,Áo
                QU002,Quần jean xanh,3,2026-05-02,250000,Quần
                """;

        ImportResultDto result = service.importFile(TENANT, csv("sales.csv", body));

        assertThat(result.status()).isEqualTo("completed");
        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.validRows()).isEqualTo(2);
        assertThat(result.skippedRows()).isZero();
        assertThat(result.productCount()).isEqualTo(2);
        assertThat(result.orderCount()).isEqualTo(2);

        // Product event MUST be published before the order event.
        InOrder ordered = inOrder(kafkaTemplate);
        ordered.verify(kafkaTemplate).send(eq(PRODUCT_TOPIC), eq(TENANT.toString()), any(ProductSyncedEvent.class));
        ordered.verify(kafkaTemplate).send(eq(ORDER_TOPIC), eq(TENANT.toString()), any(OrderSyncedEvent.class));
    }

    @Test
    void productAndOrderPayloads_areConsistent() {
        String body = """
                sku,ten_san_pham,so_luong_da_ban,ngay_giao_dich,gia_ban
                AO001,Áo thun,4,2026-05-01,100000
                """;

        service.importFile(TENANT, csv("sales.csv", body));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(PRODUCT_TOPIC), anyString(), captor.capture());
        ProductSyncedEvent productEvent = (ProductSyncedEvent) captor.getValue();
        assertThat(productEvent.getConnectorType()).isEqualTo("FILE");
        assertThat(productEvent.getProducts()).hasSize(1);
        ProductSyncedEvent.SyncedProductPayload p = productEvent.getProducts().get(0);
        assertThat(p.getSku()).isEqualTo("AO001");
        assertThat(p.getExternalId()).startsWith("FILE_");

        ArgumentCaptor<Object> orderCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(ORDER_TOPIC), anyString(), orderCaptor.capture());
        OrderSyncedEvent orderEvent = (OrderSyncedEvent) orderCaptor.getValue();
        OrderSyncedEvent.SyncedOrderLine line = orderEvent.getOrders().get(0).getLines().get(0);
        // line.productCode MUST equal product.sku for catalog SKU resolution
        assertThat(line.getProductCode()).isEqualTo(p.getSku());
        assertThat(line.getQuantity()).isEqualTo(4);
    }

    @Test
    void unsupportedJsonFormat_throwsBusinessException_noEmit() {
        MultipartFile json = new MockMultipartFile(
                "file", "data.json", "application/json", "[]".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.importFile(TENANT, json))
                .isInstanceOf(BusinessException.class);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void tooManyInvalidRows_throwsValidation_noEmit() {
        // Header has no required columns → every row is skipped.
        String body = """
                ghi_chu,mau_sac
                abc,đỏ
                def,xanh
                ghi,vàng
                """;

        assertThatThrownBy(() -> service.importFile(TENANT, csv("bad.csv", body)))
                .isInstanceOf(ValidationException.class);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void emptyFile_throwsValidation_noEmit() {
        // Header only, zero data rows.
        String body = "sku,ten_san_pham,so_luong_da_ban,ngay_giao_dich\n";

        assertThatThrownBy(() -> service.importFile(TENANT, csv("empty.csv", body)))
                .isInstanceOf(ValidationException.class);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void duplicateSku_dedupesProducts_keepsAllOrders() {
        String body = """
                sku,ten_san_pham,so_luong_da_ban,ngay_giao_dich,gia_ban
                AO001,Áo thun,5,2026-05-01,150000
                AO001,Áo thun,2,2026-05-03,150000
                """;

        ImportResultDto result = service.importFile(TENANT, csv("sales.csv", body));

        assertThat(result.validRows()).isEqualTo(2);
        assertThat(result.productCount()).isEqualTo(1);  // deduped by SKU
        assertThat(result.orderCount()).isEqualTo(2);    // one order per sales row

        verify(kafkaTemplate, times(1)).send(eq(PRODUCT_TOPIC), anyString(), any());
        verify(kafkaTemplate, times(1)).send(eq(ORDER_TOPIC), anyString(), any());
    }
}
