package com.udea.service_platform.modules.users.repository;

import com.udea.service_platform.modules.users.model.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderTypeRepository extends JpaRepository<ProviderType, Long> {
}
