package com.udea.service_platform.modules.reservations.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notification foundation for reservation reschedules. For now it only logs
 * the change for both parties; a future notification module can replace or
 * complement this listener without touching the reservation service.
 */
@Slf4j
@Component
public class ReservaReprogramadaEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservaReprogramada(ReservaReprogramadaEvent event) {
        log.info("Reserva {} reprogramada por usuario {}: {} -> {} "
                        + "(cliente={}, proveedor={}) en {}",
                event.reservaId(),
                event.actorId(),
                event.horaAnterior(),
                event.horaNueva(),
                event.clienteId(),
                event.proveedorId(),
                event.timestamp());
    }
}
