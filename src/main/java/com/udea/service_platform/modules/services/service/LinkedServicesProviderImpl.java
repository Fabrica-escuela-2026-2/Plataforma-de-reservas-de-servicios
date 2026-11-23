package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.resources.service.LinkedService;
import com.udea.service_platform.modules.resources.service.LinkedServicesProvider;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LinkedServicesProviderImpl implements LinkedServicesProvider {

    private final ServiceRepository serviceRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<LinkedService>> findLinkedServices(Collection<Long> idsRecursos) {
        if (idsRecursos.isEmpty()) {
            return Map.of();
        }
        return serviceRepository.findByIdRecursosIn(idsRecursos).stream()
                .collect(Collectors.groupingBy(Service::getIdRecursos, LinkedHashMap::new,
                        Collectors.mapping(LinkedServicesProviderImpl::toLinkedService, Collectors.toList())));
    }

    private static LinkedService toLinkedService(Service service) {
        return new LinkedService(service.getId(), service.getNombre(), service.getActivo());
    }
}
