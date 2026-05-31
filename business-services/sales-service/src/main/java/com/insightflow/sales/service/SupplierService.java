package com.insightflow.sales.service;

import com.insightflow.sales.dto.request.CreateSupplierRequest;
import com.insightflow.sales.dto.response.SupplierResponse;
import com.insightflow.sales.entity.Supplier;
import com.insightflow.sales.mapper.SupplierMapper;
import com.insightflow.sales.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Transactional(readOnly = true)
    public Page<SupplierResponse> getSuppliers(UUID tenantId, Pageable pageable) {
        return supplierRepository.findByTenantId(tenantId, pageable)
                .map(supplierMapper::toResponse);
    }

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request, UUID tenantId) {
        Supplier supplier = new Supplier();
        supplier.setTenantId(tenantId);
        supplier.setName(request.getName());
        supplier.setContactName(request.getContactName());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setStatus("active");

        Supplier saved = supplierRepository.save(supplier);
        log.debug("Created supplier id={} tenantId={}", saved.getId(), tenantId);
        return supplierMapper.toResponse(saved);
    }
}
