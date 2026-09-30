package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.users.dto.ClientDetailResponse;
import com.udea.service_platform.modules.users.dto.UserRequest;
import com.udea.service_platform.modules.users.dto.UserResponse;
import com.udea.service_platform.modules.users.model.Role;
import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.RoleRepository;
import com.udea.service_platform.modules.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private Role clienteRole;
    private Role proveedorRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        clienteRole = Role.builder().idRol(1L).nombre("Cliente").build();
        proveedorRole = Role.builder().idRol(2L).nombre("Proveedor de Servicios").build();
        adminRole = Role.builder().idRol(3L).nombre("Administrador").build();
    }

    @Test
    void register_withClienteRole_success() {
        UserRequest request = UserRequest.builder()
                .nombre("Juan")
                .correo("juan@test.com")
                .password("password123")
                .idRol(1L)
                .build();

        when(userRepository.existsByCorreo("juan@test.com")).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(clienteRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        User savedUser = User.builder()
                .idUsuario(1L)
                .nombre("Juan")
                .correo("juan@test.com")
                .password("encoded_password")
                .role(clienteRole)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("Juan", response.getNombre());
        assertEquals("juan@test.com", response.getCorreo());
        assertEquals("Cliente", response.getRole().getNombre());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withProveedorRole_success() {
        UserRequest request = UserRequest.builder()
                .nombre("Maria")
                .correo("maria@test.com")
                .password("password123")
                .idRol(2L)
                .build();

        when(userRepository.existsByCorreo("maria@test.com")).thenReturn(false);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(proveedorRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        User savedUser = User.builder()
                .idUsuario(2L)
                .nombre("Maria")
                .correo("maria@test.com")
                .password("encoded_password")
                .role(proveedorRole)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("Proveedor de Servicios", response.getRole().getNombre());
    }

    @Test
    void register_withAdminRole_throwsIllegalArgument() {
        UserRequest request = UserRequest.builder()
                .nombre("Admin")
                .correo("admin@test.com")
                .password("password123")
                .idRol(3L)
                .build();

        when(userRepository.existsByCorreo("admin@test.com")).thenReturn(false);
        when(roleRepository.findById(3L)).thenReturn(Optional.of(adminRole));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(request)
        );

        assertEquals("El rol seleccionado no está permitido para auto-registro", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withNonExistentRole_throwsIllegalArgument() {
        UserRequest request = UserRequest.builder()
                .nombre("Test")
                .correo("test@test.com")
                .password("password123")
                .idRol(999L)
                .build();

        when(userRepository.existsByCorreo("test@test.com")).thenReturn(false);
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(request)
        );

        assertEquals("El rol seleccionado no existe", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withDuplicateEmail_throwsIllegalArgument() {        UserRequest request = UserRequest.builder()
                .nombre("Juan")
                .correo("existing@test.com")
                .password("password123")
                .idRol(1L)
                .build();

        when(userRepository.existsByCorreo("existing@test.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(request)
        );

        assertEquals("El correo ya está en uso", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getClientDetail_withClienteRole_returnsDetail() {
        User cliente = User.builder()
                .idUsuario(6L)
                .nombre("Juan")
                .apellido("Pérez")
                .correo("juan@test.com")
                .telefono("3001234567")
                .numeroDocumento("123456789")
                .idTipoDocumento(1L)
                .idCiudad(1L)
                .estadoCuenta("ACTIVA")
                .role(clienteRole)
                .build();
        when(userRepository.findById(6L)).thenReturn(Optional.of(cliente));

        ClientDetailResponse response = userService.getClientDetail(6L);

        assertNotNull(response);
        assertEquals(6L, response.getIdUsuario());
        assertEquals("Juan", response.getNombre());
        assertEquals("juan@test.com", response.getCorreo());
    }

    @Test
    void getClientDetail_withProveedorRole_throwsIllegalArgument() {
        User proveedor = User.builder()
                .idUsuario(2L)
                .nombre("Maria")
                .correo("maria@test.com")
                .role(proveedorRole)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(proveedor));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getClientDetail(2L)
        );

        assertEquals("El usuario no tiene rol de Cliente", exception.getMessage());
    }

    @Test
    void getClientDetail_withNonExistentId_throwsIllegalArgument() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getClientDetail(999L)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
    }
}
