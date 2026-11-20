package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ContactLookupServiceImpl implements ContactLookupService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ContactInfo> findContactsByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getIdUsuario, this::toContactInfo));
    }

    private ContactInfo toContactInfo(User user) {
        return new ContactInfo(
                user.getIdUsuario(),
                nombreCompleto(user),
                user.getCorreo(),
                user.getTelefono());
    }

    /** Joins name and surname skipping the missing ones (never prints "null"). */
    private String nombreCompleto(User user) {
        return Stream.of(user.getNombre(), user.getApellido())
                .filter(parte -> parte != null && !parte.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }
}
