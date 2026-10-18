package com.udea.service_platform.modules.reservations.mapper;

import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.users.dto.ReservaSummaryResponse;

import java.time.LocalTime;

/**
 * Single Responsibility: owns all Reserva <-> DTO transformations,
 * including EstadoReserva id -> label mapping.
 * Services orchestrate; this class formats.
 */
public final class ReservaMapper {

    private ReservaMapper() {}

    public static Reserva toEntity(Service servicio, Long idUsuario, LocalTime fechaInicio) {
        return Reserva.builder()
                .servicio(servicio)
                .idUsuario(idUsuario)
                .fechaInicio(fechaInicio)
                .idEstado(EstadoReserva.ACTIVA.getId())
                .valorAcordado(servicio.getPrecio())
                .duracionAcordada(servicio.getDuracion())
                .build();
    }

    public static ReservaResponse toResponse(Reserva reserva, String servicioNombre,
                                             String clienteNombre, String clienteCorreo) {
        return ReservaResponse.builder()
                .id(reserva.getId())
                .servicioNombre(servicioNombre)
                .clienteNombre(clienteNombre)
                .clienteCorreo(clienteCorreo)
                .fechaInicio(reserva.getFechaInicio())
                .estado(EstadoReserva.fromId(reserva.getIdEstado()).getLabel())
                .valorAcordado(reserva.getValorAcordado())
                .duracionAcordada(reserva.getDuracionAcordada())
                .build();
    }

    public static ReservaResponse toNewReservationResponse(Reserva reserva, String servicioNombre,
                                                           String clienteNombre, String clienteCorreo) {
        return ReservaResponse.builder()
                .id(reserva.getId())
                .servicioNombre(servicioNombre)
                .clienteNombre(clienteNombre)
                .clienteCorreo(clienteCorreo)
                .fechaInicio(reserva.getFechaInicio())
                .estado(EstadoReserva.ACTIVA.getLabel())
                .valorAcordado(reserva.getValorAcordado())
                .duracionAcordada(reserva.getDuracionAcordada())
                .build();
    }

    public static ReservaSummaryResponse toSummaryResponse(Reserva reserva) {
        return ReservaSummaryResponse.builder()
                .id(reserva.getId())
                .servicioNombre(reserva.getServicio().getNombre())
                .fechaInicio(reserva.getFechaInicio())
                .estado(EstadoReserva.fromId(reserva.getIdEstado()).getLabel())
                .build();
    }
}
