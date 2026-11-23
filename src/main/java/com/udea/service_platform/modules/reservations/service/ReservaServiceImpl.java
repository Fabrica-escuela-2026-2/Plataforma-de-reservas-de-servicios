package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import com.udea.service_platform.modules.notifications.service.NotificationService;
import com.udea.service_platform.modules.reservations.dto.AvailabilitySlotResponse;
import com.udea.service_platform.modules.reservations.dto.EstadoSlot;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaRescheduleRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.dto.ReservaStatusUpdateRequest;
import com.udea.service_platform.modules.reservations.dto.RescheduleSlotQuery;
import com.udea.service_platform.modules.reservations.event.ReservaEstadoCambiadoEvent;
import com.udea.service_platform.modules.reservations.event.ReservaReprogramadaEvent;
import com.udea.service_platform.modules.reservations.mapper.ReservaMapper;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.service.ServiceLookupService;
import com.udea.service_platform.modules.users.dto.ReservaSummaryResponse;
import com.udea.service_platform.modules.users.service.AccountLookupService;
import com.udea.service_platform.modules.users.service.ClientDisplayInfo;
import com.udea.service_platform.modules.users.service.ClientLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    static final String MSG_HORARIO_NO_RESERVABLE = "Ese horario no se puede reservar";
    static final String MSG_HORARIO_NO_DISPONIBLE =
            "El horario ya no está disponible. Por favor elige otro horario.";
    static final String MSG_SERVICIO_NO_DISPONIBLE = "El servicio no está disponible";

    private final ReservaRepository reservaRepository;
    private final ServiceLookupService serviceLookupService;
    private final ClientLookupService clientLookupService;
    private final ReservaCancellationPolicy cancellationPolicy;
    private final ReservaStatusTransitionPolicy statusTransitionPolicy;
    private final ReservaReschedulePolicy reschedulePolicy;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AccountLookupService accountLookupService;
    private final AvailabilityService availabilityService;
    private final NotificationService notificationService;
    private final Clock clock;

    /**
     * HU-15. Runs in one transaction: the service row is locked first, so two
     * clients booking the same service are processed one after the other and
     * the second one sees the reservation of the first (and is rejected).
     * The reservation and both notices are saved together or not at all.
     */
    @Override
    @Transactional
    public ReservaResponse createReservation(ReservaRequest request, Long clientUserId) {
        LocalDate fecha = parseFecha(request.getFecha());
        LocalTime hora = parseHora(request.getFechaInicio());
        Instant horaInicio = fecha.atTime(hora).atZone(TimeConfig.ZONA_BOGOTA).toInstant();

        if (horaInicio.isBefore(clock.instant().truncatedTo(ChronoUnit.MINUTES))) {
            throw new IllegalArgumentException(MSG_HORARIO_NO_RESERVABLE);
        }

        com.udea.service_platform.modules.services.model.Service servicio =
                serviceLookupService.lockActiveService(request.getIdServicio());

        if (!accountLookupService.isAccountActive(servicio.getIdProveedor())) {
            throw new IllegalArgumentException(MSG_SERVICIO_NO_DISPONIBLE);
        }

        if (servicio.getIdProveedor().equals(clientUserId)) {
            throw new IllegalArgumentException("No puedes reservar tu propio servicio");
        }

        assertSlotBookable(servicio.getId(), fecha, hora);

        ClientDisplayInfo client = clientLookupService.findClientDisplayInfo(clientUserId);

        Reserva saved = reservaRepository.save(ReservaMapper.toEntity(servicio, clientUserId, horaInicio));

        notifyReservationCreated(servicio, client, clientUserId, fecha, hora);

        return ReservaMapper.toNewReservationResponse(
                saved,
                servicio.getNombre(),
                client.nombreCompleto(),
                client.correo());
    }

    private LocalDate parseFecha(String fecha) {
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)");
        }
    }

    private LocalTime parseHora(String hora) {
        try {
            return LocalTime.parse(hora);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de hora inválido. Use HH:mm (ej. 14:30)");
        }
    }

    /**
     * The slot must be one of the slots the availability endpoint offers and
     * must be free. Reusing the availability calculation guarantees that what
     * the client saw as "libre" is exactly what can be booked: it already
     * applies the provider's weekly schedule, the provider's blocks, the
     * active reservations and the "not in the past" rule.
     */
    private void assertSlotBookable(Long serviceId, LocalDate fecha, LocalTime hora) {
        EstadoSlot estado = availabilityService.getAvailability(serviceId, fecha).getSlots().stream()
                .filter(slot -> hora.equals(slot.getHoraInicio()))
                .map(AvailabilitySlotResponse::getEstado)
                .findFirst()
                .orElse(null);

        if (estado == null || estado == EstadoSlot.BLOQUEADA) {
            throw new IllegalArgumentException(MSG_HORARIO_NO_RESERVABLE);
        }
        if (estado == EstadoSlot.OCUPADA) {
            throw new ConflictException(MSG_HORARIO_NO_DISPONIBLE);
        }
    }

    private void notifyReservationCreated(com.udea.service_platform.modules.services.model.Service servicio,
                                          ClientDisplayInfo client, Long clientUserId,
                                          LocalDate fecha, LocalTime hora) {
        String cuando = fecha + " a las " + hora.truncatedTo(ChronoUnit.MINUTES);

        notificationService.notifyUser(clientUserId, TipoNotificacion.RESERVA_CONFIRMADA,
                "Tu reserva de \"" + servicio.getNombre() + "\" quedó registrada para el " + cuando + ".");

        notificationService.notifyUser(servicio.getIdProveedor(), TipoNotificacion.NUEVA_RESERVA,
                "Nueva reserva de " + client.nombreCompleto() + " para \"" + servicio.getNombre()
                        + "\" el " + cuando + ".");
    }

    @Override
    @Transactional
    public ReservaResponse cancelReservation(Long reservationId, Long actorUserId) {
        Reserva reserva = reservaRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));

        ReservaCancellationPolicy.CancellationActor actor =
                cancellationPolicy.resolveActor(reserva, actorUserId);
        cancellationPolicy.validateCancellation(reserva, actor);

        EstadoReserva estadoAnterior = EstadoReserva.fromId(reserva.getIdEstado());

        reserva.setIdEstado(EstadoReserva.CANCELADA.getId());
        Reserva saved = reservaRepository.save(reserva);

        applicationEventPublisher.publishEvent(new ReservaEstadoCambiadoEvent(
                saved.getId(),
                actorUserId,
                estadoAnterior,
                EstadoReserva.CANCELADA,
                Instant.now()));

        log.info("Reserva {} cancelada por usuario {} como {}",
                reservationId, actorUserId, actor);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReservaResponse updateStatus(Long reservationId,
                                        ReservaStatusUpdateRequest request,
                                        Long actorUserId) {
        Reserva reserva = reservaRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));

        if (reserva.getServicio() == null
                || !Objects.equals(reserva.getServicio().getIdProveedor(), actorUserId)) {
            throw new AccessDeniedException(
                    "No tienes permiso para actualizar esta reserva");
        }

        EstadoReserva estadoNuevo = statusTransitionPolicy.parseTarget(request.getEstado());
        statusTransitionPolicy.validate(reserva, estadoNuevo);

        EstadoReserva estadoAnterior = EstadoReserva.fromId(reserva.getIdEstado());

        reserva.setIdEstado(estadoNuevo.getId());
        Reserva saved = reservaRepository.save(reserva);

        applicationEventPublisher.publishEvent(new ReservaEstadoCambiadoEvent(
                saved.getId(),
                actorUserId,
                estadoAnterior,
                estadoNuevo,
                Instant.now()));

        log.info("Reserva {} actualizada de {} a {} por proveedor {}",
                reservationId, estadoAnterior, estadoNuevo, actorUserId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReservaResponse rescheduleReservation(Long reservationId,
                                                 ReservaRescheduleRequest request,
                                                 Long actorUserId) {
        Reserva reserva = reservaRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));

        reschedulePolicy.validateActor(reserva, actorUserId);
        reschedulePolicy.validateState(reserva);

        LocalDate fecha = reschedulePolicy.parseFecha(request.getFecha());
        LocalTime hora = reschedulePolicy.parseHora(request.getFechaInicio());

        Integer duracionServicio = reserva.getServicio().getDuracion();
        int duracionServicioSegura = duracionServicio != null ? duracionServicio : 0;
        int duracionReserva = reserva.getDuracionAcordada() != null
                ? reserva.getDuracionAcordada()
                : duracionServicioSegura;

        if (duracionReserva <= 0 || duracionServicioSegura <= 0) {
            throw new ConflictException("La reserva no tiene una duración válida");
        }

        availabilityService.validateRescheduleSlot(new RescheduleSlotQuery(
                reserva.getServicio().getId(),
                reserva.getServicio().getIdProveedor(),
                fecha,
                hora,
                duracionReserva,
                duracionServicioSegura,
                reserva.getId()));

        Instant horaAnterior = reserva.getHoraInicio();
        Instant horaNueva = fecha.atTime(hora)
                .atZone(TimeConfig.ZONA_BOGOTA).toInstant();

        // HU-17: only the start time changes. Price, duration, owner, service
        // and state are preserved exactly as agreed.
        reserva.setHoraInicio(horaNueva);
        Reserva saved = reservaRepository.save(reserva);

        applicationEventPublisher.publishEvent(new ReservaReprogramadaEvent(
                saved.getId(),
                actorUserId,
                saved.getIdUsuario(),
                saved.getServicio().getIdProveedor(),
                horaAnterior,
                horaNueva,
                Instant.now()));

        log.info("Reserva {} reprogramada por usuario {}: {} -> {}",
                reservationId, actorUserId, horaAnterior, horaNueva);

        return toResponse(saved);
    }

    private ReservaResponse toResponse(Reserva reserva) {
        ClientDisplayInfo client = clientLookupService
                .findClientDisplayInfoByIds(Set.of(reserva.getIdUsuario()))
                .get(reserva.getIdUsuario());

        return ReservaMapper.toResponse(
                reserva,
                reserva.getServicio().getNombre(),
                client != null ? client.nombreCompleto() : "Unknown",
                client != null ? client.correo() : "");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaResponse> getProviderReservations(Long providerId, Pageable pageable) {
        Page<Reserva> page = reservaRepository.findByProviderIdOrderByFechaInicioDesc(providerId, pageable);

        Map<Long, ClientDisplayInfo> clients = clientLookupService.findClientDisplayInfoByIds(
                page.getContent().stream()
                        .map(Reserva::getIdUsuario)
                        .collect(Collectors.toSet()));

        return page.map(reserva -> {
            ClientDisplayInfo client = clients.get(reserva.getIdUsuario());
            return ReservaMapper.toResponse(
                    reserva,
                    reserva.getServicio().getNombre(),
                    client != null ? client.nombreCompleto() : "Unknown",
                    client != null ? client.correo() : "");
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaSummaryResponse> getClientReservations(Long clienteId, Pageable pageable) {
        clientLookupService.findClientDisplayInfo(clienteId);

        return reservaRepository.findByIdUsuarioOrderByFechaInicioDesc(clienteId, pageable)
                .map(ReservaMapper::toSummaryResponse);
    }
}
