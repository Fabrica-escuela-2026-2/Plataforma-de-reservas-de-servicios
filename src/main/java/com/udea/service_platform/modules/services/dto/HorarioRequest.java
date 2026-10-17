package com.udea.service_platform.modules.services.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioRequest {

    private Integer idDiaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
