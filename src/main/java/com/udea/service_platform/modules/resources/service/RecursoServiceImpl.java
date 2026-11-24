package com.udea.service_platform.modules.resources.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.resources.dto.RecursoRequest;
import com.udea.service_platform.modules.resources.dto.RecursoResponse;
import com.udea.service_platform.modules.resources.model.Recurso;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RecursoServiceImpl implements RecursoService {

    private final RecursoRepository recursoRepository;
    private final LinkedServicesProvider linkedServicesProvider;
    private final ResourceReservationGuard resourceReservationGuard;
    private final Clock clock;

    public RecursoServiceImpl(RecursoRepository recursoRepository,
                              LinkedServicesProvider linkedServicesProvider,
                              ResourceReservationGuard resourceReservationGuard,
                              Clock clock) {
        this.recursoRepository = recursoRepository;
        this.linkedServicesProvider = linkedServicesProvider;
        this.resourceReservationGuard = resourceReservationGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RecursoResponse crear(RecursoRequest request, Long idProveedorToken, boolean esAdmin) {
        Long idProveedor = resolverProveedor(request.getIdProveedor(), idProveedorToken, esAdmin);
        String nombre = normalizarNombre(request.getNombre());
        validarNombre(nombre);
        if (recursoRepository.existsByIdProveedorAndNombreIgnoreCase(idProveedor, nombre)) {
            throw new ConflictException("Ya existe un recurso con ese nombre");
        }
        Recurso recurso = Recurso.builder()
                .nombre(nombre)
                .idProveedor(idProveedor)
                .activo(true)
                .build();
        return aResponse(recursoRepository.save(recurso), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecursoResponse> misRecursos(Long idProveedor) {
        return aResponses(recursoRepository.findByIdProveedorOrderByNombreAsc(idProveedor));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecursoResponse> listar(Long idProveedorFiltro) {
        if (idProveedorFiltro == null) {
            return aResponses(recursoRepository.findAllByOrderByNombreAsc());
        } else {
            return aResponses(recursoRepository.findByIdProveedorOrderByNombreAsc(idProveedorFiltro));
        }
    }

    @Override
    @Transactional
    public RecursoResponse inactivar(Long idRecurso, Long idProveedor, boolean esAdmin) {
        Recurso recurso = recursoRepository.findById(idRecurso)
                .orElseThrow(() -> new ResourceNotFoundException("El recurso no existe"));
        if (!esAdmin && !Objects.equals(recurso.getIdProveedor(), idProveedor)) {
            throw new AccessDeniedException("El recurso no pertenece al proveedor");
        }
        if (Boolean.FALSE.equals(recurso.getActivo())) {
            return aResponse(recurso, List.of());
        }
        if (resourceReservationGuard.hasFutureOccupyingReservations(idRecurso, Instant.now(clock))) {
            throw new ConflictException("El recurso tiene reservas pendientes. Resuélvelas antes de darlo de baja.");
        }
        recurso.setActivo(false);
        return aResponse(recursoRepository.save(recurso), List.of());
    }

    private Long resolverProveedor(Long idProveedorBody, Long idProveedorToken, boolean esAdmin) {
        if (esAdmin) {
            if (idProveedorBody == null) {
                throw new IllegalArgumentException("Debes indicar el proveedor");
            } else {
                return idProveedorBody;
            }
        } else {
            return idProveedorToken;
        }
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null) {
            return "";
        } else {
            return nombre.trim();
        }
    }

    private void validarNombre(String nombre) {
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre del recurso es obligatorio");
        }
    }

    private List<RecursoResponse> aResponses(List<Recurso> recursos) {
        List<Long> ids = recursos.stream().map(Recurso::getIdRecursos).toList();
        Map<Long, List<LinkedService>> servicios = linkedServicesProvider.findLinkedServices(ids);
        return recursos.stream()
                .map(recurso -> aResponse(recurso,
                        servicios.getOrDefault(recurso.getIdRecursos(), List.of())))
                .toList();
    }

    private RecursoResponse aResponse(Recurso recurso, List<LinkedService> servicios) {
        return RecursoResponse.builder()
                .id(recurso.getIdRecursos())
                .nombre(recurso.getNombre())
                .idProveedor(recurso.getIdProveedor())
                .activo(recurso.getActivo())
                .servicios(servicios)
                .build();
    }
}
