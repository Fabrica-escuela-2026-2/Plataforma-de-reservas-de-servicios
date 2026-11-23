package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountLookupServiceImpl implements AccountLookupService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountActive(Long userId) {
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(user -> User.ESTADO_ACTIVA.equals(user.getEstadoCuenta()))
                .orElse(false);
    }
}
