package com.insightflow.sales.service;

import com.insightflow.sales.dto.request.CreateCustomerRequest;
import com.insightflow.sales.dto.response.CustomerResponse;
import com.insightflow.sales.entity.Customer;
import com.insightflow.sales.exception.DuplicateResourceException;
import com.insightflow.sales.exception.ResourceNotFoundException;
import com.insightflow.sales.mapper.CustomerMapper;
import com.insightflow.sales.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request, UUID tenantId) {
        if (StringUtils.hasText(request.getPhone())
                && customerRepository.findByTenantIdAndPhone(tenantId, request.getPhone()).isPresent()) {
            throw new DuplicateResourceException("Customer with phone already exists: " + request.getPhone());
        }

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setFullName(request.getFullName());
        customer.setGender(request.getGender());

        Customer saved = customerRepository.save(customer);
        log.debug("Created customer id={} tenantId={}", saved.getId(), tenantId);
        return customerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(UUID tenantId, Pageable pageable) {
        return customerRepository.findByTenantId(tenantId, pageable)
                .map(customerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id, UUID tenantId) {
        return customerRepository.findByTenantIdAndId(tenantId, id)
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional
    public void updateRfmSegment(UUID customerId, String segment, UUID tenantId) {
        Customer customer = customerRepository.findByTenantIdAndId(tenantId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        customer.setRfmSegment(segment);
        customerRepository.save(customer);
    }
}
