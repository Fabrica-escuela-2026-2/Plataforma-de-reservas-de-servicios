package com.udea.service_platform.modules.reservations.mapper;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.users.dto.ReservaSummaryResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Single Responsibility: owns all Reserva <-> DTO transformations,
 * including EstadoReserva id -> label mapping and the Instant (UTC) <->
 * local Bogota time conversions. Services orchestrate; this class formats.
 * All time conversions are null-safe: hora_inicio may be NULL on old rows.
 */
public final class ReservaMapper {

    private ReservaMapper() {}

    public static Reserva toEntity(Service servicio, Long idUsuario, Instant horaInicio) {
        return Reserva.builder()
                .servicio(servicio)
                .idUsuario(idUsuario)
                .horaInicio(horaInicio)
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
                .fechaInicio(toFechaInicio(reserva))
                .fecha(toFecha(reserva))
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
                .fechaInicio(toFechaInicio(reserva))
                .fecha(toFecha(reserva))
                .estado(EstadoReserva.ACTIVA.getLabel())
                .valorAcordado(reserva.getValorAcordado())
                .duracionAcordada(reserva.getDuracionAcordada())
                .build();
    }

    public static ReservaSummaryResponse toSummaryResponse(Reserva reserva) {
        return ReservaSummaryResponse.builder()
                .id(reserva.getId())
                .servicioNombre(reserva.getServicio().getNombre())
                .fechaInicio(toFechaInicio(reserva))
                .fecha(toFecha(reserva))
                .estado(EstadoReserva.fromId(reserva.getIdEstado()).getLabel())
                .build();
    }

    private static LocalTime toFechaInicio(Reserva reserva) {
        if (reserva.getHoraInicio() == null) {
            return null;
        }
        return reserva.getHoraInicio().atZone(TimeConfig.ZONA_BOGOTA).toLocalTime();
    }

    private static LocalDate toFecha(Reserva reserva) {
        if (reserva.getHoraInicio() == null) {
            return null;
        }
        return reserva.getHoraInicio().atZone(TimeConfig.ZONA_BOGOTA).toLocalDate();
    }
}
