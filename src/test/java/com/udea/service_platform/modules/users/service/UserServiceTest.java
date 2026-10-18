package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.users.dto.ClientDetailResponse;
import com.udea.service_platform.modules.users.dto.ProfileUpdateRequest;
import com.udea.service_platform.modules.users.dto.ProviderOnboardingRequest;
import com.udea.service_platform.modules.users.dto.UserProfileResponse;
import com.udea.service_platform.modules.users.dto.UserRequest;
import com.udea.service_platform.modules.users.dto.UserResponse;
import com.udea.service_platform.modules.users.model.Role;
import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.ProviderTypeRepository;
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
    private ProviderTypeRepository providerTypeRepository;

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

    private User buildProveedor() {
        return User.builder()
                .idUsuario(5L)
                .nombre("Maria")
                .apellido("Gomez")
                .correo("maria@test.com")
                .telefono("3000000000")
                .idTipoDocumento(1L)
                .numeroDocumento("123456789")
                .idCiudad(1L)
                .estadoCuenta("ACTIVA")
                .role(proveedorRole)
                .build();
    }

    @Test
    void onboardProvider_withValidData_success() {
        User proveedor = buildProveedor();
        when(userRepository.findById(5L)).thenReturn(Optional.of(proveedor));
        when(providerTypeRepository.existsById(2L)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(proveedor);

        ProviderOnboardingRequest request = ProviderOnboardingRequest.builder()
                .idTipoProveedor(2L)
                .telefono("3001112233")
                .notasEspeciales("Barbero con 5 años de experiencia")
                .build();

        UserProfileResponse response = userService.onboardProvider(5L, request);

        assertNotNull(response);
        assertEquals(5L, response.getIdUsuario());
        assertEquals(2L, response.getIdTipoProveedor());
        assertEquals("3001112233", response.getTelefono());
        assertEquals("Barbero con 5 años de experiencia", response.getNotasEspeciales());
        assertEquals("ACTIVA", response.getEstadoCuenta());
        assertEquals("Proveedor de Servicios", response.getRole().getNombre());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void onboardProvider_withNonExistentProviderType_throwsIllegalArgument() {
        User proveedor = buildProveedor();
        when(userRepository.findById(5L)).thenReturn(Optional.of(proveedor));
        when(providerTypeRepository.existsById(999L)).thenReturn(false);

        ProviderOnboardingRequest request = ProviderOnboardingRequest.builder()
                .idTipoProveedor(999L)
                .telefono("3001112233")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.onboardProvider(5L, request)
        );

        assertEquals("El tipo de proveedor no existe", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void onboardProvider_withBlankPhone_throwsIllegalArgument() {
        ProviderOnboardingRequest request = ProviderOnboardingRequest.builder()
                .idTipoProveedor(2L)
                .telefono("   ")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.onboardProvider(5L, request)
        );

        assertEquals("El teléfono es obligatorio", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void onboardProvider_withNonProviderRole_throwsIllegalArgument() {
        User cliente = User.builder()
                .idUsuario(6L)
                .nombre("Juan")
                .correo("juan@test.com")
                .estadoCuenta("ACTIVA")
                .role(clienteRole)
                .build();
        when(userRepository.findById(6L)).thenReturn(Optional.of(cliente));

        ProviderOnboardingRequest request = ProviderOnboardingRequest.builder()
                .idTipoProveedor(2L)
                .telefono("3001112233")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.onboardProvider(6L, request)
        );

        assertEquals("El usuario no tiene rol de Proveedor", exception.getMessage());
        verify(providerTypeRepository, never()).existsById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void onboardProvider_withUnknownUser_throwsIllegalArgument() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ProviderOnboardingRequest request = ProviderOnboardingRequest.builder()
                .idTipoProveedor(2L)
                .telefono("3001112233")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.onboardProvider(999L, request)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    private User buildFullUser() {
        return User.builder()
                .idUsuario(5L)
                .nombre("Maria")
                .apellido("Gomez")
                .correo("maria@test.com")
                .password("hash_secreto")
                .telefono("3000000000")
                .idTipoDocumento(1L)
                .numeroDocumento("123456789")
                .idCiudad(2L)
                .idTipoProveedor(9L)
                .notasEspeciales("Bio original")
                .estadoCuenta("INACTIVA")
                .role(proveedorRole)
                .build();
    }

    @Test
    void getProfile_success() {
        User user = buildFullUser();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfile(5L);

        assertNotNull(response);
        assertEquals(5L, response.getIdUsuario());
        assertEquals("Maria", response.getNombre());
        assertEquals("Gomez", response.getApellido());
        assertEquals("maria@test.com", response.getCorreo());
        assertEquals("3000000000", response.getTelefono());
        assertEquals(1L, response.getIdTipoDocumento());
        assertEquals("123456789", response.getNumeroDocumento());
        assertEquals(2L, response.getIdCiudad());
        assertEquals(9L, response.getIdTipoProveedor());
        assertEquals("Bio original", response.getNotasEspeciales());
        assertEquals("INACTIVA", response.getEstadoCuenta());
        assertEquals("Proveedor de Servicios", response.getRole().getNombre());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getProfile_userNotFound_throwsIllegalArgument() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getProfile(999L)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_partialUpdate_keepsExistingFields() {
        User user = buildFullUser();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .telefono("3111111111")
                .notasEspeciales("Nueva bio")
                .build();

        UserProfileResponse response = userService.updateProfile(5L, request);

        assertEquals("3111111111", response.getTelefono());
        assertEquals("Nueva bio", response.getNotasEspeciales());

        assertEquals("Maria", response.getNombre());
        assertEquals("Gomez", response.getApellido());
        assertEquals(1L, response.getIdTipoDocumento());
        assertEquals("123456789", response.getNumeroDocumento());
        assertEquals(2L, response.getIdCiudad());

        assertEquals("maria@test.com", response.getCorreo());
        assertEquals("INACTIVA", response.getEstadoCuenta());
        assertEquals(9L, response.getIdTipoProveedor());
        assertEquals("Proveedor de Servicios", response.getRole().getNombre());

        verify(userRepository, never()).existsCiudad(any());
        verify(userRepository, never()).existsTipoDocumento(any());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_success() {
        User user = buildFullUser();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.existsCiudad(3L)).thenReturn(true);
        when(userRepository.existsTipoDocumento(1L)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .nombre("Pedro")
                .apellido("Rios")
                .telefono("3222222222")
                .idCiudad(3L)
                .idTipoDocumento(1L)
                .numeroDocumento("987654321")
                .notasEspeciales("Bio actualizada")
                .build();

        UserProfileResponse response = userService.updateProfile(5L, request);

        assertEquals("Pedro", response.getNombre());
        assertEquals("Rios", response.getApellido());
        assertEquals("3222222222", response.getTelefono());
        assertEquals(3L, response.getIdCiudad());
        assertEquals(1L, response.getIdTipoDocumento());
        assertEquals("987654321", response.getNumeroDocumento());
        assertEquals("Bio actualizada", response.getNotasEspeciales());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_withUnknownCity_throwsIllegalArgument() {
        User user = buildFullUser();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.existsCiudad(99L)).thenReturn(false);

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .idCiudad(99L)
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateProfile(5L, request)
        );

        assertEquals("La ciudad seleccionada no existe", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_withUnknownDocumentType_throwsIllegalArgument() {
        User user = buildFullUser();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.existsTipoDocumento(99L)).thenReturn(false);

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .idTipoDocumento(99L)
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateProfile(5L, request)
        );

        assertEquals("El tipo de documento no existe", exception.getMessage());
        verify(userRepository, never()).existsCiudad(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_unknownUser_throwsIllegalArgument() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .nombre("Pedro")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateProfile(999L, request)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_withBlankNombre_throwsIllegalArgument() {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .nombre("   ")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateProfile(5L, request)
        );

        assertEquals("El nombre no puede estar vacío", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }
}
