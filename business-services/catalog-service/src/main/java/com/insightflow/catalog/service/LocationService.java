package com.insightflow.catalog.service;

import com.insightflow.catalog.dto.request.CreateLocationRequest;
import com.insightflow.catalog.dto.response.LocationResponse;
import com.insightflow.catalog.entity.Location;
import com.insightflow.catalog.mapper.LocationMapper;
import com.insightflow.catalog.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Transactional(readOnly = true)
    public List<LocationResponse> getActiveLocations(UUID tenantId) {
        return locationRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .map(locationMapper::toResponse)
                .toList();
    }

    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request, UUID tenantId) {
        Location location = new Location();
        location.setTenantId(tenantId);
        location.setName(request.getName());
        location.setType(request.getType());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setActive(true);

        Location saved = locationRepository.save(location);
        log.debug("Created location id={} tenantId={}", saved.getId(), tenantId);
        return locationMapper.toResponse(saved);
    }
}
