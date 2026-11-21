package com.udea.service_platform.modules.reservations.controller;

import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
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
