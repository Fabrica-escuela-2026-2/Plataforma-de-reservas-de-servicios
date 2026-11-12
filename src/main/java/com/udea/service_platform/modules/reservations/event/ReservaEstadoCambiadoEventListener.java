package com.udea.service_platform.modules.reservations.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notification foundation for reservation state changes. For now it only
 * logs the event; a future notification module can replace or complement this
 * listener without touching the reservation service.
 */
@Slf4j
@Component
public class ReservaEstadoCambiadoEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservaEstadoCambiado(ReservaEstadoCambiadoEvent event) {
        log.info("Reserva {} cambió de {} a {} por usuario {} en {}",
                event.reservaId(),
                event.estadoAnterior(),
                event.estadoNuevo(),
                event.actorId(),
                event.timestamp());
    }
}
