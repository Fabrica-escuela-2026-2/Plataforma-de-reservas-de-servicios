package com.udea.service_platform.modules.reservations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One row of "Mis reservas" (HU-16). Same field names as ReservaResponse:
 * {@code fecha} is the day and {@code fechaInicio} the start time (Bogota).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiReservaResponse {

    private Long id;
    private String servicioNombre;
    private LocalDate fecha;
    private LocalTime fechaInicio;
    private Integer duracionAcordada;
    private BigDecimal valorAcordado;
    private String estado;
    /** true when the reservation is confirmed and has not started yet. */
    private Boolean pendiente;
    private ContactoResponse contacto;
}
