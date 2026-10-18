package com.udea.service_platform.modules.services.mapper;

import com.udea.service_platform.modules.services.dto.BloqueoRequest;
import com.udea.service_platform.modules.services.dto.BloqueoResponse;
import com.udea.service_platform.modules.services.model.Bloqueo;

public final class BloqueoMapper {

    private BloqueoMapper() {
    }

    public static Bloqueo toEntity(BloqueoRequest request, Long idUsuarioProveedor) {
        return Bloqueo.builder()
                .idUsuarioProveedor(idUsuarioProveedor)
                .fecha(request.getFecha())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .motivo(request.getMotivo())
                .build();
    }

    public static BloqueoResponse toResponse(Bloqueo bloqueo) {
        return BloqueoResponse.builder()
                .idBloqueo(bloqueo.getIdBloqueo())
                .idUsuarioProveedor(bloqueo.getIdUsuarioProveedor())
                .fecha(bloqueo.getFecha())
                .horaInicio(bloqueo.getHoraInicio())
                .horaFin(bloqueo.getHoraFin())
                .motivo(bloqueo.getMotivo())
                .build();
    }
}
