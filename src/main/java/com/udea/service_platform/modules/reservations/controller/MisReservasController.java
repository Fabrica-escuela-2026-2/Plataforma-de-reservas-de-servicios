package com.udea.service_platform.modules.reservations.controller;

import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.reservations.dto.MisReservasResponse;
import com.udea.service_platform.modules.reservations.service.MisReservasService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-16 "Mis reservas": same endpoint for both roles. A client gets the
 * reservations he made (with the provider's contact); a provider gets the
 * reservations of his services (with each client's contact). Newest first.
 * Anyone else (e.g. an admin) is rejected with 403 and anonymous callers with 401.
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class MisReservasController {

    private static final String ROL_CLIENTE = "CLIENTE";
    private static final int MAX_PAGE_SIZE = 50;

    private final MisReservasService misReservasService;

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('CLIENTE', 'PROVEEDOR')")
    public ResponseEntity<MisReservasResponse> getMyReservations(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        if (ROL_CLIENTE.equals(userDetails.getRol())) {
            return ResponseEntity.ok(
                    misReservasService.getMyReservationsAsClient(userDetails.getIdUsuario(), pageable));
        }
        return ResponseEntity.ok(
                misReservasService.getMyReservationsAsProvider(userDetails.getIdUsuario(), pageable));
    }
}
