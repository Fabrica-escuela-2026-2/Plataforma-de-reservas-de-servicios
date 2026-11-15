package com.udea.service_platform.modules.reservations.controller;

import com.udea.service_platform.modules.core.exception.ForbiddenOperationException;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.service.ReservaService;
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

    private static final String ROL_CLIENTE = "CLIENTE";

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponse> createReservation(
            @Valid @RequestBody ReservaRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (!ROL_CLIENTE.equals(userDetails.getRol())) {
            throw new ForbiddenOperationException("Solo los clientes pueden agendar citas");
        }
        ReservaResponse response = reservaService.createReservation(request, userDetails.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
