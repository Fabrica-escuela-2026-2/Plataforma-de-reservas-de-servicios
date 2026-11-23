package com.udea.service_platform.modules.reservations.mapper;

import com.udea.service_platform.modules.reservations.dto.ContactoResponse;
import com.udea.service_platform.modules.reservations.dto.MiReservaResponse;
import com.udea.service_platform.modules.reservations.dto.MisReservasResponse;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.users.service.ContactInfo;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * HU-16: transformations of "Mis reservas". Date/time conversions (UTC Instant
 * to Bogota local date and time) are shared with {@link ReservaMapper}.
 */
public final class MisReservasMapper {

    private MisReservasMapper() {
    }

    /**
     * One row of "Mis reservas". {@code ahora} decides if the reservation is
     * still pending; {@code contacto} is the other party of the reservation
     * (build it with {@link #toContacto}).
     */
    public static MiReservaResponse toMiReservaResponse(Reserva reserva, ContactoResponse contacto,
                                                        Instant ahora) {
        return MiReservaResponse.builder()
                .id(reserva.getId())
                .servicioNombre(reserva.getServicio().getNombre())
                .fecha(ReservaMapper.toFecha(reserva))
                .fechaInicio(ReservaMapper.toFechaInicio(reserva))
                .duracionAcordada(reserva.getDuracionAcordada())
                .valorAcordado(reserva.getValorAcordado())
                .estado(EstadoReserva.fromId(reserva.getIdEstado()).getLabel())
                .pendiente(isPendiente(reserva, ahora))
                .contacto(contacto)
                .build();
    }

    /**
     * Pending = ACTIVA or CONFIRMADA and not started yet (the current minute still
     * counts). EN_PROCESO (already started), CANCELADA, COMPLETADA and NO_ASISTIO
     * reservations, reservations whose start time already passed and legacy rows
     * without start time are "past".
     */
    static boolean isPendiente(Reserva reserva, Instant ahora) {
        return (EstadoReserva.ACTIVA.getId().equals(reserva.getIdEstado())
                || EstadoReserva.CONFIRMADA.getId().equals(reserva.getIdEstado()))
                && reserva.getHoraInicio() != null
                && !reserva.getHoraInicio().isBefore(ahora.truncatedTo(ChronoUnit.MINUTES));
    }

    /**
     * Contact of the other party. When the user no longer exists the reservation
     * is still listed, with a placeholder name and no correo/telefono.
     */
    public static ContactoResponse toContacto(String rol, ContactInfo info) {
        if (info == null) {
            return ContactoResponse.builder().rol(rol).nombre("No disponible").build();
        }
        return ContactoResponse.builder()
                .rol(rol)
                .nombre(info.nombreCompleto())
                .correo(info.correo())
                .telefono(info.telefono())
                .build();
    }

    public static MisReservasResponse toMisReservasResponse(Page<MiReservaResponse> page) {
        return MisReservasResponse.builder()
                .mensaje(page.getTotalElements() == 0 ? MisReservasResponse.MENSAJE_SIN_RESERVAS : null)
                .content(page.getContent())
                .number(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
