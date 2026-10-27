
package com.udea.service_platform.modules.categories.service;

import com.udea.service_platform.modules.categories.dto.CategoriaRequest;
import com.udea.service_platform.modules.categories.dto.CategoriaResponse;
import com.udea.service_platform.modules.categories.model.Categoria;
import com.udea.service_platform.modules.categories.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaServiceImpl categoriaService;

    private CategoriaRequest request;

    @BeforeEach
    void setUp() {
        request = CategoriaRequest.builder()
                .nombre("Estética")
                .descripcion("Servicios de belleza y cuidado personal")
                .build();
    }

    @Test
    void createCategory_withNewName_success() {
        // Arrange
        when(categoriaRepository.existsByNombreIgnoreCase("Estética"))
                .thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(
                Categoria.builder()
                        .id(1L)
                        .nombre("Estética")
                        .descripcion("Servicios de belleza y cuidado personal")
                        .build()
        );

        // Act
        CategoriaResponse response = categoriaService.createCategory(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Estética", response.getNombre());
        assertEquals(
                "Servicios de belleza y cuidado personal",
                response.getDescripcion()
        );
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void createCategory_withDuplicateName_throwsAndDoesNotSave() {
        // Arrange
        when(categoriaRepository.existsByNombreIgnoreCase("Estética"))
                .thenReturn(true);

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.createCategory(request)
        );

        // Assert
        assertEquals("La categoría ya existe", ex.getMessage());
        verify(categoriaRepository).existsByNombreIgnoreCase("Estética");
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void createCategory_withDuplicateNameIgnoringCase_throwsAndDoesNotSave() {
        // Arrange
        request.setNombre("ESTÉTICA");
        when(categoriaRepository.existsByNombreIgnoreCase("ESTÉTICA"))
                .thenReturn(true);

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.createCategory(request)
        );

        // Assert
        assertEquals("La categoría ya existe", ex.getMessage());
        verify(categoriaRepository).existsByNombreIgnoreCase("ESTÉTICA");
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void createCategory_trimsNameBeforeCheckingAndSaving() {
        // Arrange
        request.setNombre("  Estética  ");
        when(categoriaRepository.existsByNombreIgnoreCase("Estética"))
                .thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        categoriaService.createCategory(request);

        // Assert
        ArgumentCaptor<Categoria> captor =
                ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        assertEquals("Estética", captor.getValue().getNombre());
        verify(categoriaRepository).existsByNombreIgnoreCase("Estética");
    }

    @Test
    void createCategory_withoutDescription_success() {
        // Arrange
        request.setDescripcion(null);
        when(categoriaRepository.existsByNombreIgnoreCase("Estética"))
                .thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoriaResponse response = categoriaService.createCategory(request);

        // Assert
        assertNotNull(response);
        assertEquals("Estética", response.getNombre());
        assertNull(response.getDescripcion());
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void getAllCategories_returnsMappedList() {
        // Arrange
        when(categoriaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(
                Categoria.builder()
                        .id(1L)
                        .nombre("Consultas")
                        .descripcion("Citas médicas")
                        .build(),
                Categoria.builder()
                        .id(2L)
                        .nombre("Estética")
                        .descripcion(null)
                        .build()
        ));

        // Act
        List<CategoriaResponse> result = categoriaService.getAllCategories();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Consultas", result.get(0).getNombre());
        assertEquals("Estética", result.get(1).getNombre());
        assertEquals("Citas médicas", result.get(0).getDescripcion());
        assertNull(result.get(1).getDescripcion());
        verify(categoriaRepository).findAllByOrderByNombreAsc();
    }

    @Test
    void getAllCategories_whenNoCategories_returnsEmptyList() {
        // Arrange
        when(categoriaRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of());

        // Act
        List<CategoriaResponse> result = categoriaService.getAllCategories();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoriaRepository).findAllByOrderByNombreAsc();
    }
}
