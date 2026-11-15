package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.dto.MiReservaResponse;
import com.udea.service_platform.modules.reservations.dto.MisReservasResponse;
import com.udea.service_platform.modules.reservations.mapper.MisReservasMapper;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.users.service.ContactInfo;
import com.udea.service_platform.modules.users.service.ContactLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MisReservasServiceImpl implements MisReservasService {

    private static final String ROL_CLIENTE = "CLIENTE";
    private static final String ROL_PROVEEDOR = "PROVEEDOR";

    private final ReservaRepository reservaRepository;
    private final ContactLookupService contactLookupService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public MisReservasResponse getMyReservationsAsClient(Long clientUserId, Pageable pageable) {
        Page<Reserva> page = reservaRepository.findByIdUsuarioOrderByFechaInicioDesc(clientUserId, pageable);
        return buildMisReservas(page, reserva -> reserva.getServicio().getIdProveedor(), ROL_PROVEEDOR);
    }

    @Override
    @Transactional(readOnly = true)
    public MisReservasResponse getMyReservationsAsProvider(Long providerUserId, Pageable pageable) {
        Page<Reserva> page = reservaRepository.findByProviderIdOrderByFechaInicioDesc(providerUserId, pageable);
        return buildMisReservas(page, Reserva::getIdUsuario, ROL_CLIENTE);
    }

    /**
     * Builds the page of "Mis reservas": resolves the contact of the other party
     * of every reservation with one batch lookup and flags the pending ones.
     */
    private MisReservasResponse buildMisReservas(Page<Reserva> page,
                                                 Function<Reserva, Long> otraParte,
                                                 String rolOtraParte) {
        Map<Long, ContactInfo> contactos = contactLookupService.findContactsByIds(
                page.getContent().stream().map(otraParte).collect(Collectors.toSet()));
        Instant ahora = clock.instant();

        Page<MiReservaResponse> reservas = page.map(reserva -> MisReservasMapper.toMiReservaResponse(
                reserva,
                MisReservasMapper.toContacto(rolOtraParte, contactos.get(otraParte.apply(reserva))),
                ahora));

        return MisReservasMapper.toMisReservasResponse(reservas);
    }
}
