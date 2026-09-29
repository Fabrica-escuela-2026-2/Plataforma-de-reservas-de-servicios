package com.udea.service_platform.modules.reservations.controller;

import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.service.ReservaService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Reservas")
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Crear una reserva")
    public ResponseEntity<ReservaResponse> createReservation(
            @Valid @RequestBody ReservaRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ReservaResponse response = reservaService.createReservation(request, userDetails.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('PROVEEDOR')")
    @Operation(summary = "Consultar las reservas del proveedor autenticado")
    public ResponseEntity<Page<ReservaResponse>> getProviderReservations(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReservaResponse> reservations = reservaService.getProviderReservations(
                userDetails.getIdUsuario(), PageRequest.of(page, size));
        return ResponseEntity.ok(reservations);
    }
}
