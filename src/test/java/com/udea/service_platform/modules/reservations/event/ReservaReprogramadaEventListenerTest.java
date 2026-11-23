package com.udea.service_platform.modules.reservations.event;

import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import com.udea.service_platform.modules.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservaReprogramadaEventListenerTest {

    private static final Long RESERVA_ID = 100L;
    private static final Long CLIENTE_ID = 2L;
    private static final Long PROVEEDOR_ID = 5L;
    private static final Instant HORA_ANTERIOR = Instant.parse("2026-10-21T14:00:00Z");
    private static final Instant HORA_NUEVA = Instant.parse("2026-10-21T15:00:00Z");

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReservaReprogramadaEventListener listener;

    @Test
    void onReservaReprogramada_notificaAlClienteYAlProveedorConElNuevoHorario() {
        ReservaReprogramadaEvent event = new ReservaReprogramadaEvent(
                RESERVA_ID, CLIENTE_ID, CLIENTE_ID, PROVEEDOR_ID,
                HORA_ANTERIOR, HORA_NUEVA, Instant.now());

        listener.onReservaReprogramada(event);

        ArgumentCaptor<String> mensajeCliente = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mensajeProveedor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(
                eq(CLIENTE_ID), eq(TipoNotificacion.RESERVA_REPROGRAMADA), mensajeCliente.capture());
        verify(notificationService).notifyUser(
                eq(PROVEEDOR_ID), eq(TipoNotificacion.RESERVA_REPROGRAMADA), mensajeProveedor.capture());

        // 15:00Z = 10:00 en Bogota (UTC-5)
        assertTrue(mensajeCliente.getValue().contains("#100"));
        assertTrue(mensajeCliente.getValue().contains("2026-10-21 10:00"));
        assertTrue(mensajeProveedor.getValue().contains("#100"));
        assertTrue(mensajeProveedor.getValue().contains("2026-10-21 10:00"));
        assertEquals(2, mensajeCliente.getAllValues().size() + mensajeProveedor.getAllValues().size());
    }

    @Test
    void onReservaReprogramada_cuandoReprogramaElProveedor_igualNotificaAAmbasPartes() {
        ReservaReprogramadaEvent event = new ReservaReprogramadaEvent(
                RESERVA_ID, PROVEEDOR_ID, CLIENTE_ID, PROVEEDOR_ID,
                HORA_ANTERIOR, HORA_NUEVA, Instant.now());

        listener.onReservaReprogramada(event);

        verify(notificationService, times(1))
                .notifyUser(eq(CLIENTE_ID), eq(TipoNotificacion.RESERVA_REPROGRAMADA), any());
        verify(notificationService, times(1))
                .notifyUser(eq(PROVEEDOR_ID), eq(TipoNotificacion.RESERVA_REPROGRAMADA), any());
    }
}