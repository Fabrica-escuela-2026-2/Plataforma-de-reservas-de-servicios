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
        when(categoriaRepository.existsByNombreIgnoreCase("Estética")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(
                Categoria.builder()
                        .id(1L)
                        .nombre("Estética")
                        .descripcion("Servicios de belleza y cuidado personal")
                        .build());

        CategoriaResponse response = categoriaService.createCategory(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Estética", response.getNombre());
        assertEquals("Servicios de belleza y cuidado personal", response.getDescripcion());
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void createCategory_withDuplicateName_throwsAndDoesNotSave() {
        when(categoriaRepository.existsByNombreIgnoreCase("Estética")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> categoriaService.createCategory(request));

        assertEquals("La categoría ya existe", ex.getMessage());
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void createCategory_trimsNameBeforeCheckingAndSaving() {
        request.setNombre("  Estética  ");
        when(categoriaRepository.existsByNombreIgnoreCase("Estética")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        categoriaService.createCategory(request);

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        assertEquals("Estética", captor.getValue().getNombre());
    }

    @Test
    void createCategory_withoutDescription_success() {
        request.setDescripcion(null);
        when(categoriaRepository.existsByNombreIgnoreCase("Estética")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaResponse response = categoriaService.createCategory(request);

        assertEquals("Estética", response.getNombre());
        assertNull(response.getDescripcion());
    }

    @Test
    void getAllCategories_returnsMappedList() {
        when(categoriaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(
                Categoria.builder().id(1L).nombre("Consultas").descripcion("Citas médicas").build(),
                Categoria.builder().id(2L).nombre("Estética").descripcion(null).build()));

        List<CategoriaResponse> result = categoriaService.getAllCategories();

        assertEquals(2, result.size());
        assertEquals("Consultas", result.get(0).getNombre());
        assertEquals("Estética", result.get(1).getNombre());
    }
}
