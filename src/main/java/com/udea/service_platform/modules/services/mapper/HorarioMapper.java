package com.udea.service_platform.modules.services.mapper;

import com.udea.service_platform.modules.services.dto.HorarioRequest;
import com.udea.service_platform.modules.services.dto.HorarioResponse;
import com.udea.service_platform.modules.services.model.Horario;

public final class HorarioMapper {

    private HorarioMapper() {
    }

    public static Horario toEntity(HorarioRequest request, Integer idServicio) {
        return Horario.builder()
                .idDiaSemana(request.getIdDiaSemana())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .idServicio(idServicio)
                .build();
    }

    public static HorarioResponse toResponse(Horario horario, String nombreDia, Long idServicio) {
        return HorarioResponse.builder()
                .idHorario(horario.getIdHorario())
                .idDiaSemana(horario.getIdDiaSemana())
                .nombreDia(nombreDia)
                .horaInicio(horario.getHoraInicio())
                .horaFin(horario.getHoraFin())
                .idServicio(idServicio)
                .build();
    }
}
