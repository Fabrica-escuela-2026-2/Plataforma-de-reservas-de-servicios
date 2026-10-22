package com.udea.service_platform.modules.users.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaSummaryResponse {

    private Long id;
    private String servicioNombre;
    private LocalTime fechaInicio;
    private LocalDate fecha;
    private String estado;
}
