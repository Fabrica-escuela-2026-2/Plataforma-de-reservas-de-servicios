package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.HorarioRequest;
import com.udea.service_platform.modules.services.dto.HorarioResponse;
import com.udea.service_platform.modules.services.mapper.HorarioMapper;
import com.udea.service_platform.modules.services.model.DiaSemana;
import com.udea.service_platform.modules.services.model.Horario;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.DiaSemanaRepository;
import com.udea.service_platform.modules.services.repository.HorarioRepository;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class HorarioServiceImpl implements HorarioService {

    private final ServiceRepository serviceRepository;
    private final HorarioRepository horarioRepository;
    private final DiaSemanaRepository diaSemanaRepository;

    @Override
    @Transactional
    public List<HorarioResponse> defineSchedule(Long serviceId, List<HorarioRequest> requests, Long providerId) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Debe definir al menos un horario para el servicio");
        }

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("El servicio no existe"));

        verificarPropietario(service, providerId);

        // El id vive en una columna integer (FK), solo una existencia verificada puede convertirse
        Integer idServicio = Math.toIntExact(serviceId);

        Map<Integer, String> dias = cargarDias();

        for (HorarioRequest request : requests) {
            if (request.getIdDiaSemana() == null || !dias.containsKey(request.getIdDiaSemana())) {
                throw new IllegalArgumentException("El día de la semana no existe");
            }
            if (request.getHoraInicio() == null || request.getHoraFin() == null) {
                throw new IllegalArgumentException("La hora de inicio y la hora de fin son obligatorias");
            }
            if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
                throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
            }
        }

        List<HorarioRequest> ordenados = requests.stream()
                .sorted(Comparator.comparing(HorarioRequest::getIdDiaSemana)
                        .thenComparing(HorarioRequest::getHoraInicio))
                .toList();

        validarSolapes(ordenados);

        // Reemplazo total: solo despues de validar TODAS las entradas
        horarioRepository.deleteByIdServicio(idServicio);

        List<Horario> guardados = horarioRepository.saveAll(
                ordenados.stream()
                        .map(request -> HorarioMapper.toEntity(request, idServicio))
                        .toList());

        return guardados.stream()
                .map(horario -> HorarioMapper.toResponse(horario, dias.get(horario.getIdDiaSemana()), serviceId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioResponse> getSchedule(Long serviceId, Long providerId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("El servicio no existe"));

        verificarPropietario(service, providerId);

        Integer idServicio = Math.toIntExact(serviceId);

        List<Horario> horarios = horarioRepository.findByIdServicio(idServicio);
        if (horarios.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> dias = cargarDias();

        return horarios.stream()
                .map(horario -> HorarioMapper.toResponse(horario, dias.get(horario.getIdDiaSemana()), serviceId))
                .toList();
    }

    private void verificarPropietario(Service service, Long providerId) {
        if (!service.getIdProveedor().equals(providerId)) {
            throw new AccessDeniedException("No tienes permiso para gestionar el horario de este servicio");
        }
    }

    private Map<Integer, String> cargarDias() {
        return diaSemanaRepository.findAll().stream()
                .collect(Collectors.toMap(DiaSemana::getId, DiaSemana::getNombre));
    }

    private void validarSolapes(List<HorarioRequest> ordenados) {
        HorarioRequest anterior = ordenados.get(0);
        for (int i = 1; i < ordenados.size(); i++) {
            HorarioRequest actual = ordenados.get(i);
            if (anterior.getIdDiaSemana().equals(actual.getIdDiaSemana())
                    && anterior.getHoraFin().isAfter(actual.getHoraInicio())) {
                throw new IllegalArgumentException("Los horarios no pueden solaparse en el mismo día");
            }
            anterior = actual;
        }
    }
}
