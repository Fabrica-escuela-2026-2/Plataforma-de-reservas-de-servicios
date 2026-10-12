package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.users.dto.ClientDetailResponse;
import com.udea.service_platform.modules.users.dto.ClientSummaryResponse;
import com.udea.service_platform.modules.users.dto.ProviderOnboardingRequest;
import com.udea.service_platform.modules.users.dto.UserProfileResponse;
import com.udea.service_platform.modules.users.dto.UserRequest;
import com.udea.service_platform.modules.users.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Dependency Inversion: controllers and other modules depend on
 * this abstraction, not on the concrete implementation.
 */
public interface UserService {

    UserResponse register(UserRequest request);

    Page<ClientSummaryResponse> searchClients(String searchTerm, Pageable pageable);

    ClientDetailResponse getClientDetail(Long id);

    UserProfileResponse onboardProvider(Long idUsuario, ProviderOnboardingRequest request);
}
