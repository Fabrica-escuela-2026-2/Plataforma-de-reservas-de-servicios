package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.BloqueoFranja;
import com.udea.service_platform.modules.services.dto.HorarioFranja;
import com.udea.service_platform.modules.services.model.Bloqueo;
import com.udea.service_platform.modules.services.model.Horario;
import com.udea.service_platform.modules.services.repository.BloqueoRepository;
import com.udea.service_platform.modules.services.repository.HorarioRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ScheduleLookupServiceImpl implements ScheduleLookupService {

    private final HorarioRepository horarioRepository;
    private final BloqueoRepository bloqueoRepository;

    @Override
    public List<HorarioFranja> findServiceSchedules(Long serviceId) {
        // La FK vive en una columna integer: solo un id verificable se convierte.
        Integer idServicio = Math.toIntExact(serviceId);

        return horarioRepository.findByIdServicio(idServicio).stream()
                .map(this::toFranja)
                .toList();
    }

    @Override
    public List<BloqueoFranja> findProviderBlocks(Long providerId, LocalDate fecha) {
        return bloqueoRepository.findByIdUsuarioProveedorAndFecha(providerId, fecha).stream()
                .map(this::toBloqueoFranja)
                .toList();
    }

    private HorarioFranja toFranja(Horario horario) {
        return new HorarioFranja(horario.getIdDiaSemana(), horario.getHoraInicio(), horario.getHoraFin());
    }

    private BloqueoFranja toBloqueoFranja(Bloqueo bloqueo) {
        return new BloqueoFranja(bloqueo.getHoraInicio(), bloqueo.getHoraFin());
    }
}
