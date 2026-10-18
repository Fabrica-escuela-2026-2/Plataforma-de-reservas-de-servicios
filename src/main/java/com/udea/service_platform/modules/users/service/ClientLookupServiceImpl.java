package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientLookupServiceImpl implements ClientLookupService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ClientDisplayInfo findClientDisplayInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!"Cliente".equals(user.getRole().getNombre())) {
            throw new IllegalArgumentException("El usuario no tiene rol de Cliente");
        }

        return toDisplayInfo(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ClientDisplayInfo> findClientDisplayInfoByIds(Collection<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .filter(user -> "Cliente".equals(user.getRole().getNombre()))
                .collect(Collectors.toMap(User::getIdUsuario, this::toDisplayInfo));
    }

    private ClientDisplayInfo toDisplayInfo(User user) {
        return new ClientDisplayInfo(
                user.getIdUsuario(),
                user.getNombre() + " " + user.getApellido(),
                user.getCorreo());
    }
}
