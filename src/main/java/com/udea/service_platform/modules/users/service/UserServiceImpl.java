package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.users.dto.*;
import com.udea.service_platform.modules.users.mapper.UserMapper;
import com.udea.service_platform.modules.users.model.Role;
import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.ClientSpecification;
import com.udea.service_platform.modules.users.repository.RoleRepository;
import com.udea.service_platform.modules.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse register(UserRequest request) {
        if (userRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalArgumentException("El correo ya está en uso");
        }

        Role role = roleRepository.findById(request.getIdRol())
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe"));

        if (!RoleConstants.ROLES_PERMITIDOS_AUTO_REGISTRO.contains(role.getNombre())) {
            throw new IllegalArgumentException("El rol seleccionado no está permitido para auto-registro");
        }

        User user = UserMapper.toEntity(request, role, passwordEncoder.encode(request.getPassword()));

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            log.error("Database error during registration for email {}: {}", request.getCorreo(), ex.getMessage(), ex);
            throw new IllegalArgumentException("Error al registrar el usuario: verifique los datos proporcionados");
        } catch (Exception ex) {
            log.error("Unexpected error during registration for email {}: {}", request.getCorreo(), ex.getMessage(), ex);
            throw new IllegalArgumentException("Error interno al registrar el usuario");
        }

        return UserMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientSummaryResponse> searchClients(String searchTerm, Pageable pageable) {
        var spec = ClientSpecification.buildFilter(searchTerm);
        return userRepository.findAll(spec, pageable)
                .map(UserMapper::toClientSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDetailResponse getClientDetail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!"Cliente".equals(user.getRole().getNombre())) {
            throw new IllegalArgumentException("El usuario no tiene rol de Cliente");
        }

        return UserMapper.toClientDetail(user);
    }
}
