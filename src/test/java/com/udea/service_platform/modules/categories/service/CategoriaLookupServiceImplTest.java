package com.udea.service_platform.modules.categories.service;

import com.udea.service_platform.modules.categories.model.Categoria;
import com.udea.service_platform.modules.categories.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaLookupServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaLookupServiceImpl categoriaLookupService;

    @Test
    void resolveOrCreate_existente_devuelveInfoSinInsertar() {
        Categoria existente = Categoria.builder().id(7L).nombre("Belleza").build();
        when(categoriaRepository.findByNombreIgnoreCase("Belleza"))
                .thenReturn(Optional.of(existente));

        CategoriaInfo info = categoriaLookupService.resolveOrCreate("  Belleza  ");

        assertEquals(7L, info.id());
        assertEquals("Belleza", info.nombre());
        verify(categoriaRepository, never()).insertIfAbsent("Belleza");
    }

    @Test
    void resolveOrCreate_inexistente_insertaYDevuelveLaCreada() {
        Categoria creada = Categoria.builder().id(9L).nombre("Nueva").build();
        when(categoriaRepository.findByNombreIgnoreCase("Nueva"))
                .thenReturn(Optional.empty(), Optional.of(creada));

        CategoriaInfo info = categoriaLookupService.resolveOrCreate("Nueva");

        assertEquals(9L, info.id());
        assertEquals("Nueva", info.nombre());
        verify(categoriaRepository).insertIfAbsent("Nueva");
    }

    @Test
    void resolveOrCreate_nombreNuloOVacio_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> categoriaLookupService.resolveOrCreate(null));
        assertThrows(IllegalArgumentException.class,
                () -> categoriaLookupService.resolveOrCreate("   "));

        verifyNoInteractions(categoriaRepository);
    }
}
