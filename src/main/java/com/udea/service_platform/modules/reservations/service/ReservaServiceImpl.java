package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import com.udea.service_platform.modules.notifications.service.NotificationService;
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
    static final String MSG_SERVICIO_NO_DISPONIBLE = "El servicio no está disponible";
    static final String MSG_SERVICIO_MODIFICADO = "El servicio fue modificado, intenta de nuevo";

    private final ReservaRepository reservaRepository;
    private final ServiceLookupService serviceLookupService;
    private final ClientLookupService clientLookupService;
    private final ReservaCancellationPolicy cancellationPolicy;
    private final ReservaStatusTransitionPolicy statusTransitionPolicy;
    private final ReservaReschedulePolicy reschedulePolicy;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AccountLookupService accountLookupService;
    private final AvailabilityService availabilityService;
    private final ReservationResourceLock resourceLock;
    private final ReservationSlotPolicy slotPolicy;
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

        com.udea.service_platform.modules.services.model.Service servicioInicial =
                findServiceForBooking(request.getIdServicio());
        Long idRecurso = servicioInicial.getIdRecursos();

        resourceLock.lockResource(idRecurso);

        com.udea.service_platform.modules.services.model.Service servicio =
                serviceLookupService.lockActiveService(request.getIdServicio());
        assertRecursoNoCambio(idRecurso, servicio);
        Integer aforo = servicio.getAforo();

        if (!accountLookupService.isAccountActive(servicio.getIdProveedor())) {
            throw new IllegalArgumentException(MSG_SERVICIO_NO_DISPONIBLE);
        }

        if (servicio.getIdProveedor().equals(clientUserId)) {
            throw new IllegalArgumentException("No puedes reservar tu propio servicio");
        }

        assertSlotBookable(servicio, aforo, fecha, hora);

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

    private com.udea.service_platform.modules.services.model.Service findServiceForBooking(Long serviceId) {
        try {
            return serviceLookupService.findActiveService(serviceId);
        } catch (ResourceNotFoundException | IllegalArgumentException ex) {
            throw new IllegalArgumentException(MSG_SERVICIO_NO_DISPONIBLE);
        }
    }

    private void assertRecursoNoCambio(Long idRecursoPrevio,
                                       com.udea.service_platform.modules.services.model.Service servicio) {
        if (!Objects.equals(idRecursoPrevio, servicio.getIdRecursos())) {
            throw new ConflictException(MSG_SERVICIO_MODIFICADO);
        }
    }

    private void assertSlotBookable(com.udea.service_platform.modules.services.model.Service servicio,
                                    Integer aforo, LocalDate fecha, LocalTime hora) {
        int duracionServicio = servicio.getDuracion() == null ? 0 : servicio.getDuracion();
        SlotContext context = slotPolicy.loadForMutation(
                servicio.getId(),
                servicio.getIdRecursos(),
                aforo,
                servicio.getIdProveedor(),
                fecha,
                duracionServicio,
                null);
        SlotDecision decision = slotPolicy.evaluate(context, hora, duracionServicio);

        switch (decision.reason()) {
            case OUT_OF_SCHEDULE, PROVIDER_BLOCK -> throw new IllegalArgumentException(MSG_HORARIO_NO_RESERVABLE);
            case RESOURCE_BUSY, CAPACITY_FULL -> throw new ConflictException(decision.mensaje());
            default -> {
            }
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
        Reserva reservaInicial = reservaRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));
        if (reservaInicial.getServicio() == null) {
            throw new ResourceNotFoundException("La reserva no existe");
        }
        Long serviceId = reservaInicial.getServicio().getId();
        Long idRecurso = reservaInicial.getServicio().getIdRecursos();

        resourceLock.lockResource(idRecurso);

        com.udea.service_platform.modules.services.model.Service servicio =
                serviceLookupService.lockService(serviceId);
        assertRecursoNoCambio(idRecurso, servicio);

        Reserva reserva = reservaRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("La reserva no existe"));

        reschedulePolicy.validateActor(reserva, actorUserId);
        reschedulePolicy.validateState(reserva);

        LocalDate fecha = reschedulePolicy.parseFecha(request.getFecha());
        LocalTime hora = reschedulePolicy.parseHora(request.getFechaInicio());

        int duracionServicioSegura = servicio.getDuracion() == null ? 0 : servicio.getDuracion();
        int duracionReserva = reserva.getDuracionAcordada() != null
                ? reserva.getDuracionAcordada()
                : duracionServicioSegura;

        if (duracionReserva <= 0 || duracionServicioSegura <= 0) {
            throw new ConflictException("La reserva no tiene una duración válida");
        }

        availabilityService.validateRescheduleSlot(new RescheduleSlotQuery(
                serviceId,
                servicio.getIdProveedor(),
                fecha,
                hora,
                duracionReserva,
                duracionServicioSegura,
                reserva.getId(),
                servicio.getIdRecursos(),
                servicio.getAforo()));

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
