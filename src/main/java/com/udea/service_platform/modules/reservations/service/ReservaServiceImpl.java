package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.mapper.ReservaMapper;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.service.ServiceLookupService;
import com.udea.service_platform.modules.users.dto.ReservaSummaryResponse;
import com.udea.service_platform.modules.users.service.ClientDisplayInfo;
import com.udea.service_platform.modules.users.service.ClientLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final ServiceLookupService serviceLookupService;
    private final ClientLookupService clientLookupService;

    @Override
    public ReservaResponse createReservation(ReservaRequest request, Long clientUserId) {
        com.udea.service_platform.modules.services.model.Service servicio =
                serviceLookupService.findActiveService(request.getIdServicio());

        if (servicio.getIdProveedor().equals(clientUserId)) {
            throw new IllegalArgumentException("No puedes reservar tu propio servicio");
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(request.getFecha());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)");
        }

        LocalTime hora;
        try {
            hora = LocalTime.parse(request.getFechaInicio());
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de hora inválido. Use HH:mm (ej. 14:30)");
        }

        Instant horaInicio = fecha.atTime(hora).atZone(TimeConfig.ZONA_BOGOTA).toInstant();

        Reserva reserva = ReservaMapper.toEntity(servicio, clientUserId, horaInicio);

        Reserva saved = reservaRepository.save(reserva);

        ClientDisplayInfo client = clientLookupService.findClientDisplayInfo(clientUserId);

        return ReservaMapper.toNewReservationResponse(
                saved,
                servicio.getNombre(),
                client.nombreCompleto(),
                client.correo());
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
