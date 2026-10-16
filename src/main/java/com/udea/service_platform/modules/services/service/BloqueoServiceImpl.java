package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.BloqueoRequest;
import com.udea.service_platform.modules.services.dto.BloqueoResponse;
import com.udea.service_platform.modules.services.mapper.BloqueoMapper;
import com.udea.service_platform.modules.services.model.Bloqueo;
import com.udea.service_platform.modules.services.repository.BloqueoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BloqueoServiceImpl implements BloqueoService {

    private final BloqueoRepository bloqueoRepository;

    @Override
    @Transactional
    public BloqueoResponse createBlock(BloqueoRequest request, Long providerId) {
        if (request.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede ser en el pasado");
        }

        boolean parcial = (request.getHoraInicio() == null) != (request.getHoraFin() == null);
        if (parcial) {
            throw new IllegalArgumentException("Debe indicar ambas horas (inicio y fin) o ninguna");
        }

        if (request.getHoraInicio() != null && !request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Bloqueo bloqueo = bloqueoRepository.save(BloqueoMapper.toEntity(request, providerId));

        return BloqueoMapper.toResponse(bloqueo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueoResponse> getActiveBlocks(Long providerId) {
        return bloqueoRepository
                .findByIdUsuarioProveedorAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                        providerId, LocalDate.now())
                .stream()
                .map(BloqueoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteBlock(Long blockId, Long providerId) {
        Bloqueo bloqueo = bloqueoRepository.findByIdBloqueoAndIdUsuarioProveedor(blockId, providerId)
                .orElseThrow(() -> new AccessDeniedException(
                        "El bloque no existe o no tienes permiso sobre él"));

        bloqueoRepository.delete(bloqueo);
    }
}
