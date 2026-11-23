package com.udea.service_platform.modules.reservations.event;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import com.udea.service_platform.modules.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * HU-17: notifica al cliente y al proveedor cuando una reserva se reprograma.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaReprogramadaEventListener {

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationService notificationService;

    // BEFORE_COMMIT (no AFTER_COMMIT): notifyUser() es @Transactional (REQUIRED). En
    // AFTER_COMMIT la transaccion original ya termino y el save() se uniria a ella sin
    // confirmarse: los avisos se perderian en silencio. En BEFORE_COMMIT los avisos se
    // guardan en la misma transaccion que la reprogramacion (todo o nada), igual que en HU-15.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onReservaReprogramada(ReservaReprogramadaEvent event) {
        String nuevaFechaHora = ZonedDateTime
                .ofInstant(event.horaNueva(), TimeConfig.ZONA_BOGOTA)
                .format(FORMATO_FECHA_HORA);

        notificationService.notifyUser(event.clienteId(), TipoNotificacion.RESERVA_REPROGRAMADA,
                "Tu reserva #" + event.reservaId() + " fue reprogramada para el "
                        + nuevaFechaHora + ".");

        notificationService.notifyUser(event.proveedorId(), TipoNotificacion.RESERVA_REPROGRAMADA,
                "La reserva #" + event.reservaId() + " fue reprogramada para el "
                        + nuevaFechaHora + ".");

        log.info("Reserva {} reprogramada por usuario {}: {} -> {} (cliente={}, proveedor={}) en {}",
                event.reservaId(), event.actorId(), event.horaAnterior(), event.horaNueva(),
                event.clienteId(), event.proveedorId(), event.timestamp());
    }
}