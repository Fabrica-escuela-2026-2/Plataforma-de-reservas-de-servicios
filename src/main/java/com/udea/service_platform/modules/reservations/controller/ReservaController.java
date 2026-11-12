package com.udea.service_platform.modules.reservations.controller;

import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaRescheduleRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.dto.ReservaStatusUpdateRequest;
import com.udea.service_platform.modules.reservations.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ReservaResponse> createReservation(
            @Valid @RequestBody ReservaRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ReservaResponse response = reservaService.createReservation(request, userDetails.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Cancelar una reserva",
            description = "Cancela una reserva en estado ACTIVA o CONFIRMADA. El cliente puede "
                    + "cancelar dentro de la ventana configurable (24 horas por defecto); el "
                    + "proveedor puede cancelar antes del inicio de la reserva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva cancelada"),
            @ApiResponse(responseCode = "403", description = "El usuario no es propietario de la reserva"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "409", description = "Estado no cancelable o fuera de la ventana de cancelación")
    })
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROVEEDOR')")
    public ResponseEntity<ReservaResponse> cancelReservation(
            @Parameter(description = "ID de la reserva", example = "100") @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ReservaResponse response = reservaService.cancelReservation(id, userDetails.getIdUsuario());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Actualizar el estado operativo de una reserva",
            description = "Permite al proveedor propietario del servicio avanzar el estado de una "
                    + "reserva: ACTIVA -> CONFIRMADA/EN_PROCESO/COMPLETADA/NO_ASISTIO, "
                    + "CONFIRMADA -> EN_PROCESO/COMPLETADA/NO_ASISTIO y EN_PROCESO -> COMPLETADA. "
                    + "Las reservas en estado terminal no se pueden modificar y la cancelación "
                    + "debe realizarse con PATCH /api/reservations/{id}/cancel.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "El usuario no es el proveedor propietario"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<ReservaResponse> updateStatus(
            @Parameter(description = "ID de la reserva", example = "100") @PathVariable Long id,
            @Valid @RequestBody ReservaStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ReservaResponse response = reservaService.updateStatus(
                id, request, userDetails.getIdUsuario());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reprogramar una reserva",
            description = "Permite al cliente propietario o al proveedor propietario mover una "
                    + "reserva en estado ACTIVA o CONFIRMADA a una nueva fecha y hora. La nueva "
                    + "franja debe estar dentro del horario del servicio, no estar bloqueada y "
                    + "no estar ocupada por otra reserva. El valor y la duración acordados se "
                    + "conservan sin cambios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva reprogramada"),
            @ApiResponse(responseCode = "400", description = "Fecha u hora ausente o inválida"),
            @ApiResponse(responseCode = "403", description = "El usuario no es cliente ni proveedor propietario"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "409", description = "Estado no reprogramable o franja no disponible")
    })
    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROVEEDOR')")
    public ResponseEntity<ReservaResponse> rescheduleReservation(
            @Parameter(description = "ID de la reserva", example = "100") @PathVariable Long id,
            @Valid @RequestBody ReservaRescheduleRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ReservaResponse response = reservaService.rescheduleReservation(
                id, request, userDetails.getIdUsuario());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('PROVEEDOR')")
    public ResponseEntity<Page<ReservaResponse>> getProviderReservations(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReservaResponse> reservations = reservaService.getProviderReservations(
                userDetails.getIdUsuario(), PageRequest.of(page, size));
        return ResponseEntity.ok(reservations);
    }
}
